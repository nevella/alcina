package com.sun.tools.jdi;

import com.sun.jdi.connect.spi.Connection;

public class VirtualMachineImplExt extends VirtualMachineImpl {
    public VirtualMachineImplExt(Connection connection) {
        super(connection);
    }
}
