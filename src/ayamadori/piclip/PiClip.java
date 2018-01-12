/*
 * PiClip.java v1.0.0
 *
 * Created on 2018/1/1
 */
package ayamadori.piclip;

import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;
import ayamadori.piclip.ui.PiPanel;

/**
 * @author Ayamadori
 * @version 1.0.0
 */
public class PiClip extends MIDlet
{
    public static int COLOR_BACKGROUND;
    public static int COLOR_FOREGROUND;
    public static int COLOR_HIGHLIGHTED_BACKGROUND;
    public static int COLOR_HIGHLIGHTED_FOREGROUND;
    
    private PiPanel piPanel;

    public PiClip()
    {
        COLOR_BACKGROUND = Display.getDisplay(this).getColor(Display.COLOR_BACKGROUND);
        COLOR_FOREGROUND = Display.getDisplay(this).getColor(Display.COLOR_FOREGROUND);
        COLOR_HIGHLIGHTED_BACKGROUND = Display.getDisplay(this).getColor(Display.COLOR_HIGHLIGHTED_BACKGROUND);
        COLOR_HIGHLIGHTED_FOREGROUND = Display.getDisplay(this).getColor(Display.COLOR_HIGHLIGHTED_FOREGROUND);
        piPanel = new PiPanel(this);
    }

    protected void startApp()
    {
        if (piPanel != null)
        {
            Display display = Display.getDisplay(this);
            display.setCurrent(piPanel);
        }
    }

    protected void pauseApp()
    {
    }

    protected void destroyApp(boolean unconditional)
    {
    }
}
