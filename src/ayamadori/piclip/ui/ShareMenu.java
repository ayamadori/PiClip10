package ayamadori.piclip.ui;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;
import javax.microedition.midlet.MIDlet;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;
import ayamadori.piclip.util.Util;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Ticker;
import javax.microedition.rms.RecordStore;
import sms.SMSScreen;

public class ShareMenu implements CommandListener
{
    private final MIDlet midlet;
    private static final String RMS_NAME = "PiClip_ShareOrder";
    private static final String[] LIST_STRING_ELEMENT =
    {
        "Search", "Twitter", "Translate", "Wikipedia", "SMS (System)", "SMS (In app)", "Mail", "Copy"
    };
    private static final int MAX_SIZE = 500;

    private Display display;
    private Displayable backScreen;
    private String text;
    private List shareList;
    private Command back;
    private final Alert errorMessageAlert;
    private byte[] order = {0, 1, 2, 3, 4, 5, 6, 7}; // Default order of list

    public ShareMenu(MIDlet midlet, String text)
    {
        this.midlet = midlet;
        this.text = text;
        loadOrder();

        try
        {
            // TODO: Order sort by learning
            shareList = new List("Share", List.IMPLICIT, LIST_STRING_ELEMENT, null);
            shareList.setTicker(new Ticker(text));
            Font font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_LARGE);
            // Sort list by order
            for (int i = 0; i < LIST_STRING_ELEMENT.length; i++)
            {
                shareList.set(order[i], LIST_STRING_ELEMENT[i], Image.createImage("/icon/" + LIST_STRING_ELEMENT[i] + ".png"));
                shareList.setFont(order[i], font);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        back = new Command("Back", Command.BACK, 1);
        shareList.addCommand(back);
        display = Display.getDisplay(midlet);
        errorMessageAlert = new Alert("Error", null, null, AlertType.ERROR);
    }

    public void showMenu(Displayable backScreen)
    {
        this.backScreen = backScreen;
        shareList.setCommandListener(this);
        display.setCurrent(shareList);
    }

    public void commandAction(Command command, Displayable displayable)
    {
        if (command == List.SELECT_COMMAND)
        {
            int index = shareList.getSelectedIndex();
            boolean success = true;
            if (index == order[0]) // Search
            {
                  success = launchURL("https://www.google.com/m?q=" + Util.URLencode(text));
            }
            else if (index == order[1]) // Twitter
            {               
                success = launchURL("https://twitter.com/intent/tweet?text=" + Util.URLencode(text));
            }
            else if (index == order[2]) // Translate
            {                
              success = launchURL("https://translate.google.com/m?q=" + Util.URLencode(text));
            }
            else if (index == order[3]) // Wikipedia
            {              
                success = launchURL("https://ja.m.wikipedia.org/w/index.php?search=" + Util.URLencode(text));
            }
            else if (index == order[4]) // SMS (System)
            {                
                success = launchURL("sms:?body=" + Util.URLencode(text));
            }
            else if (index == order[5]) // SMS (In app)
            {
                new SMSScreen(display, shareList, text);
                System.gc();
            }
            else if (index == order[6]) // Mail
            {
                success = launchURL("mailto:?body=" + Util.URLencode(text));
            }
            else if (index == order[7]) // Copy
            { 
                if (text.length() > MAX_SIZE)
                {
                    errorMessageAlert.setString("Text must be less than " + MAX_SIZE + " characters");
                    display.setCurrent(errorMessageAlert, backScreen);
                    return;
                }
                TextBox tb = new TextBox("Copy", text, MAX_SIZE, TextField.ANY);
                Command tbback = new Command("Back", Command.BACK, 1);
                tb.addCommand(tbback);
                tb.setCommandListener(new CommandListener()
                {
                    public void commandAction(Command c, Displayable d)
                    {
                        display.setCurrent(shareList);
                    }
                });
                display.setCurrent(tb);
                System.gc();
            }
            
            // Sort order for next launch -> byte[] is "Sanshou Watashi"
            if (success)
            {
                byte[] newOrder = new byte[LIST_STRING_ELEMENT.length];
                for (int i = 0; i < newOrder.length; i++)
                {
                    if (order[i] == index) newOrder[i] = 0;
                    else if (order[i] < index) newOrder[i] = (byte) (order[i] + 1);
                    else if (order[i] > index) newOrder[i] = order[i];
                }
                saveOrder(newOrder);
            }
        } else if (command == back)
        {
            display.setCurrent(backScreen);
        }
    }

    private boolean launchURL(String url)
    {
        boolean success = true;
        try
        {
            midlet.platformRequest(url);
        }
        catch (Exception e)
        {
            success = false;
            errorMessageAlert.setString("Failed to launch");
            display.setCurrent(errorMessageAlert, shareList);
            e.printStackTrace();
        }
        return success;
    }

    // 設定の取得
    private void loadOrder()
    {
        RecordStore record = null;
        try
        {
            record = RecordStore.openRecordStore(RMS_NAME, false);
            byte[] temp = record.getRecord(1);
            if (temp.length == LIST_STRING_ELEMENT.length)
            {
                order = temp;
            }
            else
            {
                record.closeRecordStore();
                RecordStore.deleteRecordStore(RMS_NAME);
            }
        } catch (Exception e)
        {
            System.err.println(e);
            try
            {
                if (record != null)
                {
                    record.closeRecordStore();
                }
            } catch (Exception e1)
            {
                System.err.println(e1);
            }
            try
            {
                if (record != null)
                {
                    RecordStore.deleteRecordStore(RMS_NAME);
                }
            } catch (Exception e2)
            {
                System.err.println(e2);
            }
        }
    }

    // 設定を保存
    public void saveOrder(byte[] newOrder)
    {
        try
        {
            RecordStore record = RecordStore.openRecordStore(RMS_NAME, true);
            if (record.getNumRecords() < 1)
            {
                record.addRecord(newOrder, 0, newOrder.length);
            } else
            {
                record.setRecord(1, newOrder, 0, newOrder.length);
            }
            record.closeRecordStore();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
