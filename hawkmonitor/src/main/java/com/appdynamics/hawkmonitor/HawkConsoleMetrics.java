package com.appdynamics.hawkmonitor;

/**
 * Created by trader on 1/15/16.
 *
 * Copyright (c) 1999-2001 TIBCO Software Inc. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * TIBCO Software Inc.
 */

import COM.TIBCO.hawk.console.hawkeye.*;
import COM.TIBCO.hawk.talon.*;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/*
 This class is provided purely for demonstration purposes. The format of its
 output is subject to change. This class illustrates  how to perform
 a method invocation and a method subscription using TIBCO Hawk's Console API.
 This class tries to retrieve the MicroAgentID for the "Self MicroAgent" from
 ANY agent in the TIBCO Hawk network. It then performs a AgentManager.describe(),
 AgentManager.invoke() and AgentManager.subscribe() and prints all its
 results to the screen.
*/

public class HawkConsoleMetrics {

    TIBHawkConsole console;
    AgentManager agentMgr;
    AgentMonitor agentMon;
    Subscription subscription;

    private static final Logger logger = Logger.getLogger("com.singularity.TibcoHawkConsoleMetricManager");

    HawkConsoleMetrics(Map<String, Object> connectProps) throws Exception {
        // Create the TIBHawkConsole Instance
        console = TIBHawkConsoleFactory.getInstance().createHawkConsole(connectProps);
        logger.info("XXX created console!");
        // retrieve and initialize AgentManager
        agentMgr = console.getAgentManager();
        logger.info("XXX get agent manager!");
        agentMgr.initialize();
        logger.info("XXX initialized agent manager!");
        agentMon = console.getAgentMonitor();
        logger.info("XXX got agent monitor!");
        agentMon.initialize();
        logger.info("XXX initialized agent monitor!");

        listMicroAgents();
        logger.info("XXX ctor done!");
    }

    public void tick() {

    }

    private void listMicroAgents() {
        MicroAgentListMonitorListener maListener = new MicroAgentListMonitorListener() {
            public void onMicroAgentAdded(MicroAgentListMonitorEvent microAgentListMonitorEvent) {
                logger.info("New micro agent: " + microAgentListMonitorEvent.getMicroAgentID().getDisplayName());
            }

            public void onMicroAgentRemoved(MicroAgentListMonitorEvent microAgentListMonitorEvent) {
                logger.info("Removed micro agent: " + microAgentListMonitorEvent.getMicroAgentID().getDisplayName());
            }
        };

        try {
            agentMon.addMicroAgentListMonitorListener(maListener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void legacy() {

        // let's try and retrieve the COM.TIBCO.hawk.talon.MicroAgentID for the
        // "Self Microagent" from ANY agent in the TIBCO Hawk network

        MicroAgentID[] maids = null;
        try {
            maids = agentMgr.getMicroAgentIDs("COM.TIBCO.hawk.microagent.Self", 1);
            if( maids.length == 0 ) {
                System.out.println("EXITING ... Unable to find any TIBCO Hawk agents\n"+
                        "for the specified RV session and Hawk domain parameters.");
                System.exit(1);
            }
            System.out.println(maids[0].toString());
        }
        catch( MicroAgentException mae ) {
            System.out.println("ERROR while performing getMicroAgentIDs: " + mae);
            System.exit(1);
        }

        // let's do a COM.TIBCO.hawk.console.hawkeye.AgentManager.describe()
        // describe() returns a COM.TIBCO.hawk.talon.MicroAgentDescriptor
        // check out the documentation of MicroAgentDescriptor

        MicroAgentDescriptor mad = null;
        try {
            mad = agentMgr.describe(maids[0]);
            System.out.println(mad.toFormattedString());
        }
        catch( MicroAgentException mae ) {
            System.out.println("ERROR while performing a describe: " + mae);
            System.exit(1);
        }

        // we know Self has a getUptime() method which takes no arguments
        // let's invoke Self.getUptime()
        String methName = "getUptime";
        MethodInvocation mi = new MethodInvocation(methName, null);
        try {
            MicroAgentData m = agentMgr.invoke(maids[0], mi);
            Object maData = m.getData();
            if( maData != null ) {
                System.out.println("\nResults of Method Invocation\n");
                printData( maData );
            }
        }
        catch( MicroAgentException me ) {
            System.out.println("ERROR while performing a method invocation: " + me);
            System.exit(1);
        }

        // subscribe to Self.getMicroAgentInfo() every 15 secs
        // we know that getMicroAgentInfo() takes an argument "Name"
        // we could examine MethodDescriptor.getArgumentDescriptor() as well !!
        DataElement[] args = new DataElement[1];
        args[0] = new DataElement("Name", new String(""));
        MethodSubscription mas = new MethodSubscription("getMicroAgentInfo", args, 15*1000);
        try {
            subscription = agentMgr.subscribe(maids[0], mas, new Subscriber(), null);
            System.out.println("\nResults of Method Subscription\n");
        }
        catch( MicroAgentException e ) {
            System.out.println("ERROR while performing a method subscription: " + e);
            System.exit(1);
        }
    }

    public void printData( Object madata )
    {
        // it could be CompositeData
        if( madata instanceof CompositeData ) {
            CompositeData compData = (CompositeData)madata;
            DataElement[] data = compData.getDataElements();

            StringBuffer sb = new StringBuffer("composite{");

            for (int i=0; i<data.length; i++)
                sb.append(data[i] + ((i==(data.length-1))?"}":", "));

            System.out.println(sb.toString());
        }
        // it could be TabularData
        else if( madata instanceof TabularData ) {
            TabularData tabData = (TabularData)madata;

            String[] columnNames = tabData.getColumnNames();
            String[] indexNames = tabData.getIndexNames();
            // alternatively you can use getAllDataElements() as well
            Object[][] table = tabData.getAllData();

            StringBuffer sb = new StringBuffer();
            sb.append("table{");
            sb.append("columns={");

            for (int i=0; i<columnNames.length; i++)
                sb.append(columnNames[i]+ ((i==(columnNames.length-1))?"} ":", "));

            sb.append("indexColumns={");

            for (int i=0; i<indexNames.length; i++)
                sb.append(indexNames[i]+ ((i==(indexNames.length-1))?"} ":", "));

            sb.append("values={");
            if (table==null)
                sb.append("null");
            else {
                for (int i=0; i<table.length; i++) {
                    sb.append("row"+i+"={");
                    for (int j=0; j<table[i].length; j++)
                        sb.append(table[i][j] + ((j==(table[i].length-1))?"} ":", "));
                }
            }
            sb.append("}");
            sb.append("}");

            System.out.println(sb.toString());
        }
        // it could be MicroAgentException .. security violations etc.
        else if( madata instanceof MicroAgentException ) {
            MicroAgentException exc = (MicroAgentException)madata;
            System.out.println("EXCEPTION: " + exc);
        }
        // it could be null - some IMPACT and IMPACT_INFO methods could return null
        else if( madata == null ) {
            System.out.println("Method Invocation returned NO data ");
        }
        // could be none of the above, possibly NOT a openMethod ??
        else {
            System.out.println("Method Invocation returned data of UNKNOWN TYPE");
        }
    }
    class Subscriber implements SubscriptionHandler
    {
        // after we get data a few times let's terminate the subscription
        int noOfTimes = 0;

        public void onData( Subscription s, MicroAgentData mad )
        {
            Object maData = mad.getData();

            if( maData != null ) {
                printData( maData );
                System.out.println("\n\n");
            }

            noOfTimes++;

            if( noOfTimes == 3 ) {
                subscription.cancel();
                System.exit(1);
            }
        }
        public void onError( Subscription s, MicroAgentException e )
        {
            System.out.println("ERROR occured during subscription: " + e);
        }
        public void onErrorCleared( Subscription s )
        {
            System.out.println("ERROR cleared resuming subscription: " + s);
        }
        public void onTermination( Subscription s, MicroAgentException e )
        {
            System.out.println("Subscription Terminated: " + e);
            System.exit(1);
        }
    }

    // Main - Usage: java ConsoleApp <hawkDomain> <rvService> <rvNetwork> <rvDaemon>
    public static void main(String[] args)
    {
        HawkConsoleMetrics ca = null;

        if (args.length != 4 ) {
            System.err.println("Usage: java ConsoleApp <hawkDomain> <rvService> <rvNetwork> <rvDaemon>");
            System.exit(1);
        }
        System.out.println("NOTICE:\n"+
                        "This utility is provided as an example use of the TIBHawkConsole API.\n"+
                        "It is intended to be used purely for demonstration and educational purposes ONLY.\n"
        );

        Map<String, Object> props = new HashMap<String, Object>();
        try {
            ca = new HawkConsoleMetrics(props);
            ca.listMicroAgents();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}