package ayamadori.piclip.util;

public class Util
{

    // http://www.s-cradle.com/developer/java/DBTestForm.html
    // Shift-JIS -> UTF-8
    /**
     * URLエンコードします。エンコード時にShift-JISを用います。
     *
     * @param in エンコードして欲しい文字列
     * @return エンコードされた文字列
     */
    public static String URLencode(String in)
    {
        StringBuffer outBuf = new StringBuffer();
        for (int i = 0; i < in.length(); i++)
        {
            char temp = in.charAt(i);
            if (('a' <= temp && temp <= 'z')
                    || ('A' <= temp && temp <= 'Z')
                    || ('0' <= temp && temp <= '9')
                    || temp == '.' || temp == '-' || temp == '*' || temp == '_')
            {
                outBuf.append(temp);
            }
            else if (temp == ' ')
            {
                outBuf.append('+');
            }
            else
            {
                byte[] bytes;
                try
                {
                    bytes = new String(new char[]
                    {
                        temp
                    }).getBytes("UTF-8");
                    for (int j = 0; j < bytes.length; j++)
                    {
                        int high = (bytes[j] >>> 4) & 0x0F;
                        int low = (bytes[j] & 0x0F);
                        outBuf.append('%');
                        outBuf.append(Integer.toString(high, 16).toLowerCase());
                        outBuf.append(Integer.toString(low, 16).toLowerCase());
                    }
                }
                catch (Exception e)
                {
                }
            }
        }
        return outBuf.toString();
    }
}
