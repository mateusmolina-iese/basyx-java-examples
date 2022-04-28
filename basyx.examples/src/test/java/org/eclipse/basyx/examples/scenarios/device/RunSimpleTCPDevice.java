/*******************************************************************************
 * Copyright (C) 2021 the Eclipse BaSyx Authors
 * 
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 * SPDX-License-Identifier: MIT
 ******************************************************************************/
package org.eclipse.basyx.examples.scenarios.device;

import static org.junit.Assert.assertTrue;

import org.eclipse.basyx.examples.contexts.BaSyxExamplesContext;
import org.eclipse.basyx.examples.deployment.BaSyxDeployment;
import org.eclipse.basyx.examples.mockup.application.ReceiveDeviceDashboardStatusApplication;
import org.eclipse.basyx.examples.mockup.device.SimpleTCPDeviceMockup;
import org.eclipse.basyx.examples.mockup.devicemanager.ManufacturingDeviceManager;
import org.eclipse.basyx.examples.support.directory.ExamplesPreconfiguredDirectory;
import org.eclipse.basyx.vab.manager.VABConnectionManager;
import org.eclipse.basyx.vab.protocol.http.connector.HTTPConnectorFactory;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * Example that illustrates a simple device (no control component), a manager,
 * an application, Asset Administration Shells, and sub models
 * 
 * This example instantiates a device (mockup), device manager, and an
 * application - Device manager registers device AAS and sub models with
 * directory server. - It also provides a TCP server that receives the device
 * status and forwards this to device status sub model - Device uses a simple,
 * string based TCP connection to the device manager - It communicates its
 * status and an invocation counter - Application connects to device AAS and
 * device status sub model and queries device status
 * 
 * @author kuhn
 *
 */
public class RunSimpleTCPDevice extends BaSyxExampleScenario {

	/**
	 * VAB connection manager backend
	 */
	protected VABConnectionManager connManager = new VABConnectionManager(new ExamplesPreconfiguredDirectory(), new HTTPConnectorFactory());

	/**
	 * Instantiate and start context elements for this example. BaSyxDeployment
	 * contexts instantiate all components on the IP address of the host. Therefore,
	 * all components use the same IP address.
	 */
	@ClassRule
	public static BaSyxDeployment context = new BaSyxDeployment(
			// Simulated servlets
			// - BaSys topology with one AAS Server and one SQL directory
			new BaSyxExamplesContext(),

			// Simulated runnables
			// - Manufacturing device manager, e.g. deployed to additonal device
			new ManufacturingDeviceManager(9998).setName("DeviceManager"),

			// Simulated mockups
			new SimpleTCPDeviceMockup(9998).setName("Device"), new ReceiveDeviceDashboardStatusApplication().setName("Application"));

	/**
	 * Test sequence: - Initialize device --> Device status update (ready) - Read
	 * device status from AAS - Invoke device service --> Device status update
	 * (running) - Read device status from AAS - Device service completes execution
	 * --> Device status update (complete) - Read device status from AAS - Device
	 * completes reset --> Device status update (idle) - Read device status from AAS
	 */
	@Test
	public void test() throws Exception {
		// Device updates status to ready
		((SimpleTCPDeviceMockup) context.getRunnable("Device")).deviceInitialized();

		// Application waits for status change
		waitfor(() -> ((ReceiveDeviceDashboardStatusApplication) context.getRunnable("Application")).getDeviceStatus().equals("IDLE"));
		assertTrue(((ReceiveDeviceDashboardStatusApplication) context.getRunnable("Application")).getDeviceStatus().equals("IDLE"));

		// Application checks invocation counter
		assertTrue(((ReceiveDeviceDashboardStatusApplication) context.getRunnable("Application")).getDeviceInvocationCounter() == 0);

		// Device updates status to running
		// - The device indicates that a process step is running
		((SimpleTCPDeviceMockup) context.getRunnable("Device")).serviceRunning();

		// Application waits for status change
		waitfor(() -> ((ReceiveDeviceDashboardStatusApplication) context.getRunnable("Application")).getDeviceStatus().equals("EXECUTE"));
		assertTrue(((ReceiveDeviceDashboardStatusApplication) context.getRunnable("Application")).getDeviceStatus().equals("EXECUTE"));

		// Device updates status to complete
		// - The device indicates that process step did finish
		((SimpleTCPDeviceMockup) context.getRunnable("Device")).serviceCompleted();

		// Application waits for status change
		waitfor(() -> ((ReceiveDeviceDashboardStatusApplication) context.getRunnable("Application")).getDeviceStatus().equals("COMPLETE"));
		assertTrue(((ReceiveDeviceDashboardStatusApplication) context.getRunnable("Application")).getDeviceStatus().equals("COMPLETE"));

		// Device updates status to ready again, next process step may be invoked
		((SimpleTCPDeviceMockup) context.getRunnable("Device")).resetCompleted();

		// Application waits for status change
		waitfor(() -> ((ReceiveDeviceDashboardStatusApplication) context.getRunnable("Application")).getDeviceStatus().equals("IDLE"));
		assertTrue(((ReceiveDeviceDashboardStatusApplication) context.getRunnable("Application")).getDeviceStatus().equals("IDLE"));

		// Application checks invocation counter
		assertTrue(((ReceiveDeviceDashboardStatusApplication) context.getRunnable("Application")).getDeviceInvocationCounter() == 1);
	}
}
