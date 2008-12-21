/*
JTestServer is a client/server framework for testing any JVM implementation.

 
Copyright (C) 2008  Fabien DUMINY (fduminy@jnode.org)

JTestServer is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

JTestServer is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/
package org.jtestserver.client.process.kvm;

import java.io.IOException;

import org.jtestserver.client.process.ServerProcess;

public class KVMServerProcess implements ServerProcess {
    private final KVMConfig config;
    private final KVM kvm; 
    
    public KVMServerProcess(KVMConfig config) {
        this.config = config;
        kvm = new KVM(config);
    }
    
    @Override
    public synchronized void start() throws IOException {
        kvm.start(config.getVmName());
    }
    
    @Override
    public synchronized void stop() throws IOException {        
        kvm.stop(config.getVmName());        
    }

    @Override
    public boolean isAlive() throws IOException {
        boolean isRunning = false;
        for (String vm : kvm.getRunningVMs()) {
            if (config.getVmName().equals(vm)) {
                isRunning = true;
                break;
            }
        }
        
        return isRunning;
    }
}
