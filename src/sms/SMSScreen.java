/*
 *
 * Copyright c 2007 Sun Microsystems, Inc. All rights reserved.
 * Use is subject to license terms.
 */
package sms;

import javax.microedition.lcdui.*;

/**
 * An example MIDlet to send text via an SMS MessageConnection
 */
public class SMSScreen implements CommandListener {

    /**
     * user interface command for indicating Send request
     */
    Command sendCommand = new Command("Send", Command.OK, 1);

    /**
     * user interface command for going back to the previous screen
     */
    Command backCommand = new Command("Back", Command.BACK, 2);

    /**
     * current display.
     */
    Display display;

    /**
     * The port on which we send SMS messages
     */
    String smsPort;

    /**
     * Area where the user enters the phone number to send the message to
     */
    TextBox destinationAddressBox;

    /**
     * Error message displayed when an invalid phone number is entered
     */
    Alert errorMessageAlert;

    /**
     * Alert that is displayed when a message is being sent
     */
    Alert sendingMessageAlert;

    /**
     * Prompts for and sends the text message
     */
    SMSSender sender;

    /**
     * Previous screen
     */
    Displayable backScreen = null;

    /**
     * Initialize the MIDlet with the current display object and graphical
     * components.
     */
    public SMSScreen(Display display, Displayable backScreen, String message) {
        this.display = display;
        this.backScreen = backScreen;

        destinationAddressBox = new TextBox("Phone Number", null, 256, TextField.PHONENUMBER);
        destinationAddressBox.addCommand(sendCommand);
        destinationAddressBox.addCommand(backCommand);
        destinationAddressBox.setCommandListener(this);

        errorMessageAlert = new Alert("SMS", null, null, AlertType.ERROR);
        errorMessageAlert.setTimeout(5000);

        sendingMessageAlert = new Alert("SMS", null, null, AlertType.INFO);
        sendingMessageAlert.setTimeout(5000);

        sender = new SMSSender(this.display, message);

        this.display.setCurrent(destinationAddressBox);
    }

    /**
     * Respond to commands, including exit
     *
     * @param c user interface command requested
     * @param s screen object initiating the request
     */
    public void commandAction(Command c, Displayable s) {
        try {
            if (c == backCommand) {
                display.setCurrent(backScreen);
            } else if (c == sendCommand) {
                promptAndSend();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Prompt for and send the message
     */
    private void promptAndSend() {
        String address = destinationAddressBox.getString();

        if (!SMSScreen.isValidPhoneNumber(address)) {
            errorMessageAlert.setString("Invalid phone number");
            display.setCurrent(errorMessageAlert, destinationAddressBox);
            return;
        }

        sender.promptAndSend(address);
        display.setCurrent(backScreen);
    }

    /**
     * Check the phone number for validity Valid phone numbers contain only the
     * digits 0 thru 9, and may contain a leading '+'.
     */
    private static boolean isValidPhoneNumber(String number) {
        char[] chars = number.toCharArray();

        if (chars.length == 0) {
            return false;
        }

        int startPos = 0;

        // initial '+' is OK
        if (chars[0] == '+') {
            startPos = 1;
        }

        for (int i = startPos; i < chars.length; ++i) {
            if (!Character.isDigit(chars[i])) {
                return false;
            }
        }

        return true;
    }
}
