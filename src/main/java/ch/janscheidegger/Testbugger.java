package ch.janscheidegger;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.BooleanType;
import com.sun.jdi.BooleanValue;
import com.sun.jdi.Bootstrap;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.connect.VMStartException;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.ExceptionEvent;
import com.sun.jdi.event.LocatableEvent;
import com.sun.jdi.event.StepEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.ExceptionRequest;
import com.sun.jdi.request.StepRequest;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;

public class Testbugger {

    private Class debugClass;
    private int[] breakPointLines;

    public static void main(String[] args) throws Exception {
        System.out.println("start");
        Testbugger debuggerInstance = new Testbugger();
        debuggerInstance.setDebugClass(Example.class);
        int[] breakPoints = {};
        debuggerInstance.setBreakPointLines(breakPoints);
        VirtualMachine vm = null;

        try {
            vm = debuggerInstance.connectAndLaunchVM();
            debuggerInstance.enableClassPrepareRequest(vm);

            EventSet eventSet = null;
            while ((eventSet = vm.eventQueue().remove()) != null) {
                for (Event event : eventSet) {
                    if (event instanceof ClassPrepareEvent) {
                        System.out.println("[ClassPrepareEvent]");
                        debuggerInstance.setBreakPoints(vm, (ClassPrepareEvent) event);
                    }
                    if(event instanceof ExceptionEvent) {
                        System.out.println("[ExpceptionEvent]");
                        System.out.println(((ExceptionEvent) event).exception());
                        debuggerInstance.displayVariables((ExceptionEvent) event);
                    }
                    vm.resume();
                }
            }
            System.out.println("end");
        } catch (VMDisconnectedException e) {
            System.out.println("Virtual Machine is disconnected.");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            InputStreamReader reader = new InputStreamReader(vm.process().getInputStream());
            OutputStreamWriter writer = new OutputStreamWriter(System.out);
            char[] buf = new char[512];

            reader.read(buf);
            writer.write(buf);
            writer.flush();
        }

    }

    public Class getDebugClass() {
        return debugClass;
    }

    public void setDebugClass(Class debugClass) {
        this.debugClass = debugClass;
    }

    public int[] getBreakPointLines() {
        return breakPointLines;
    }

    public void setBreakPointLines(int[] breakPointLines) {
        this.breakPointLines = breakPointLines;
    }

    /**
     * Sets the debug class as the main argument in the connector and launches the VM
     *
     * @return VirtualMachine
     * @throws IOException
     * @throws IllegalConnectorArgumentsException
     * @throws VMStartException
     */
    public VirtualMachine connectAndLaunchVM() throws IOException, IllegalConnectorArgumentsException, VMStartException {
        LaunchingConnector launchingConnector = Bootstrap.virtualMachineManager().defaultConnector();
        Map<String, Connector.Argument> arguments = launchingConnector.defaultArguments();
        arguments.get("main").setValue(debugClass.getName());
        VirtualMachine vm = launchingConnector.launch(arguments);
        System.out.println(vm);
        return vm;
    }

    /**
     * Creates a request to prepare the debug class, add filter as the debug class and enables it
     *
     * @param vm
     */
    public void enableClassPrepareRequest(VirtualMachine vm) {
        ClassPrepareRequest classPrepareRequest = vm.eventRequestManager().createClassPrepareRequest();
        classPrepareRequest.addClassFilter(debugClass.getName());
        classPrepareRequest.enable();
    }

    /**
     * Sets the break points at the line numbers mentioned in breakPointLines array
     *
     * @param vm
     * @param event
     */
    public void setBreakPoints(VirtualMachine vm, ClassPrepareEvent event) {
        ExceptionRequest exceptionBreakpoint = vm.eventRequestManager().createExceptionRequest(null, false, true);
        exceptionBreakpoint.enable();
    }

    /**
     * Displays the visible variables
     *
     * @param event
     * @throws IncompatibleThreadStateException
     * @throws AbsentInformationException
     */
    public void displayVariables(LocatableEvent event) throws IncompatibleThreadStateException, AbsentInformationException {
        ObjectReference objectReference = event.thread().frame(0).thisObject();
        System.out.println(objectReference);
        String name = objectReference.referenceType().name();
        System.out.println("[REFERENCENAME]" + name);

        System.out.println("[METHOD]");
        System.out.println(event.location().method().name());
        event.location().method().arguments().forEach(loc -> {
            try {
                System.out.println(loc.name()+ event.thread().frame(0).getValue(loc));
            } catch (IncompatibleThreadStateException e) {
                e.printStackTrace();
            }
        });

        System.out.println("[FIELDS]");
        for (Field field : objectReference.referenceType().allFields()) {
            Value value = objectReference.getValue(field);
            System.out.println(field.typeName());
            System.out.println(field.declaringType());

            if(value instanceof BooleanValue) {
                System.out.println(field.name() + "=" + ((BooleanValue) value).value());
            }
        }
    }
}
