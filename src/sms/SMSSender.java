/*
 *
 * Copyright ﾂｩ 2007 Sun Microsystems, Inc. All rights reserved.
 * Use is subject to license terms.
 */
package sms;

import javax.microedition.io.*;
import javax.microedition.lcdui.*;
import javax.wireless.messaging.*;

/**
 * Prompts for text and sends it via an SMS MessageConnection
 */
public class SMSSender implements Runnable {

    /**
     * Display to use.
     */
    Display display;

    /**
     * The port on which we send SMS messages
     */
    String message;

    /**
     * The URL to send the message to
     */
    String destinationAddress;

    /**
     * Initialize the MIDlet with the current display object and graphical
     * components.
     */
    public SMSSender(Display display, String message) {
        this.display = display;
        this.message = message;
        this.destinationAddress = null;
    }

    /**
     * Prompt for message and send it
     */
    public void promptAndSend(String destinationAddress) {
        this.destinationAddress = destinationAddress;
        new Thread(this).start();
    }

    /**
     * Send the message. Called on a separate thread so we don't have contention
     * for the display
     */
    public void run() {
//		デフォルトのSMSを送信するため、ポート番号は指定しない
        String address = "sms://" + destinationAddress;

        MessageConnection smsconn = null;

        try {
            /**
             * Open the message connection.
             */
            smsconn = (MessageConnection) Connector.open(address);

            TextMessage txtmessage = (TextMessage) smsconn.newMessage(MessageConnection.TEXT_MESSAGE);
            txtmessage.setAddress(address);
            txtmessage.setPayloadText(message);
            smsconn.send(txtmessage);
        } catch (Exception t) {
            System.out.println("Send caught: ");
            showInfo("Error", "Send caught: " + t.toString());
            t.printStackTrace();
        } finally {
            try {
                if (smsconn != null) {
                    smsconn.close();
                }
            } catch (Exception ioe) {
                System.out.println("Closing connection caught: ");
                showInfo("Error", "Closing connection caught: " + ioe.toString());
                ioe.printStackTrace();
            }
        }
        showInfo("SMS", "Sent message to " + destinationAddress);
    }

    private void showInfo(String title, String mes) {
        Alert al = new Alert(title);
        al.setString(mes);
        al.setTimeout(Alert.FOREVER);
        display.setCurrent(al);
    }
}
