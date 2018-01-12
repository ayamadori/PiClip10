/**
 * Dictionary.java v1.0.0
 * Created on 2018/01/01
 */
package ayamadori.piclip.dic;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.microedition.rms.RecordStore;

/**
 * 辞書
 *
 * @author ayamadori
 */
public abstract class Dictionary
{

    // 辞書検索モード用定数
    // 通常予測モード
    public static final int PREDICT_NORMAL = 0;
    // 完全一致変換モード
    public static final int PREDICT_FULL_MATCH = 1;
    public static final int MAX_CANDIDATE = 36;
    protected static final int SPLIT = 128;

    protected int[] dicLine;
    protected int candLength;
    protected int[] candDicLine;
    protected StringBuffer dicData;
    protected int searchMode;

    private int dicIndex;
    private int histories[];
    private String name;

    private RecordStore record;

    public Dictionary(String name)
    {

        this.name = name;
        // StringBufferを使った基本辞書。最初から50KB確保
        dicData = new StringBuffer(1024 * 50);//Hold 50kB
        // 辞書index
        dicIndex = -1;
        // 現在の予測候補数
        candLength = 0;
        // No include original word in Candidates
        candDicLine = new int[MAX_CANDIDATE];
        // 辞書検索モード
        searchMode = PREDICT_NORMAL;
        try
        {
            record = RecordStore.openRecordStore(name, true);
            System.out.println("RMSsize=" + record.getSize());
            if (record.getNumRecords() < 128)
            {
                byte b[] = new byte[1];
                for (int i = 0; i < SPLIT; i++)
                {
                    record.addRecord(b, 0, b.length);
                }
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    protected void loadDic(int index)
    {
        if (dicIndex != index)
        {
            dicIndex = index;
        } else
        {
            return;
        }
        // reset dictionary
        if (dicData.length() > 0)
        {
            dicData.delete(0, dicData.length());
        }
        InputStream is = null;
        byte[] buffer = new byte[8192];
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos = new ByteArrayOutputStream();
        try
        {
            String postfix = Integer.toHexString(index).toUpperCase();
            if (postfix.length() < 2)
            {
                postfix = "0" + postfix;
            }
            is = getClass().getResourceAsStream("/dic/" + name + "/" + name + postfix);
            int size;
            while ((size = is.read(buffer)) > -1)
            {
                baos.write(buffer, 0, size);
            }
            if (baos.size() > 0)
            {
                byte[] data = baos.toByteArray();
                String temp = new String(data, "UTF-8");
                dicLine = new int[temp.length()];
                dicLine[0] = 0;
                int enter = 0, start = 0, i = 1;
                while ((enter = temp.indexOf('\n', start)) > -1 && enter < temp.length() - 1)
                {
                    start = enter + 1;
                    dicLine[i] = start;
                    i++;
                }
                loadHisory();
                if (histories.length > 0)
                {
                    for (i = 0; i < histories.length; i++)
                    {
                        start = dicLine[histories[i]];
                        int end = temp.indexOf('\n', start);
                        char[] tempChars = new char[end - start + 1];
                        temp.getChars(start, end + 1, tempChars, 0);
                        dicData.append(tempChars);
                    }
                    enter = 0;
                    start = 0;
                    i = 1;
                    while ((enter = indexOf(dicData, '\n', start)) > -1 && enter < dicData.length() - 1)
                    {
                        start = enter + 1;
                        dicLine[i] = start;
                        i++;
                    }
                } else
                {
                    dicData.append(temp);
                }
                temp = null;
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        } finally
        {
            try
            {
                is.close();
                baos.close();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    // 予測エンジン
    public abstract void search(String prefix, String yomi, String[] cand, int start);

    // 現在の予測候補数を返す
    public int getCandsSize()
    {
        return candLength;
    }

    // 文字を平仮名の清音に変換
    protected char toHiraSei(char ch)
    {
        // 片仮名を平仮名に変換
        if (ch >= '\u30a1' && ch <= '\u30f4')
        {
            ch -= 96;
        }
        // 濁音、半濁音、小文字を大文字の清音に変換
        if (((ch >= '\u3041' && ch <= '\u304a') || (ch >= '\u3083' && ch <= '\u3088')) && ch % 2 != 0)
        {
            ch++;
        } else if (ch >= '\u304b' && ch <= '\u3062' && ch % 2 == 0)
        {
            ch--;
        } else if (ch == '\u3063' || ch == '\u3065')
        {
            ch = '\u3064';
        } else if (ch == '\u3067' || ch == '\u3069')
        {
            ch--;
        } else if (ch >= '\u306f' && ch <= '\u307d' && ch % 3 != 0)
        {
            if (ch % 3 == 2)
            {
                ch -= 2;
            } else if (ch % 3 == 1)
            {
                ch--;
            }
        } else if (ch == '\u308e')
        {
            ch++;
        }
        return ch;
    }

    // ---------------------------------------------------------------------------------------------
    // 現在の検索モードを返す
    public int searchMode()
    {
        return searchMode;
    }

    // ---------------------------------------------------------------------------
    // 検索モードを設定
    public void setSearchMode(int mode)
    {
        searchMode = (mode == PREDICT_FULL_MATCH) ? PREDICT_FULL_MATCH : PREDICT_NORMAL;
    }

    // -----------------------------------------------------------------------------------
    // 候補を確定したときその候補を最上段に移動＝学習機能
    public void learning(int candNumber)
    {
        if (candNumber >= candLength || candDicLine[candNumber] == 0)
        {
            return;
        }
        // initialize histories
        if (histories.length == 0)
        {
            boolean first = true;
            for (int i = dicLine.length; i > 1; i--)
            {
                if (dicLine[i - 1] > 0)
                {
                    if (first)
                    {
                        histories = new int[i];
                        histories[0] = 0;
                        first = false;
                    }
                    histories[i - 1] = i - 1;
                }
            }
        }
        int temp = histories[candDicLine[candNumber]];
        System.arraycopy(histories, 0, histories, 1, candDicLine[candNumber]);
        histories[0] = temp;
        saveHistory();
        dicIndex = -1;
    }

    // -----------------------------------------------------------------------------
    // Stringを使うとメモリ消費が激しいのでStringBufferだけで済ませる
    protected int indexOf(StringBuffer sb, char ch, int start)
    {
        if (start < 0)
        {
            start = 0;
        }
        while (start < sb.length())
        {
            if (sb.charAt(start) == ch)
            {
                return start;
            }
            start++;
        }
        return -1;
    }

    // Stringを使うとメモリ消費が激しいのでStringBufferだけで済ませる
    protected int lastIndexOf(StringBuffer sb, char ch, int start)
    {
        if (start > sb.length() - 1)
        {
            start = sb.length() - 1;
        }
        while (start > -1)
        {
            if (sb.charAt(start) == ch)
            {
                return start;
            }
            start--;
        }
        return -1;
    }

    // ---------------------------------------------------------------------
    // Load history
    private void loadHisory()
    {
        try
        {
            byte[] byteData = record.getRecord(dicIndex + 1);
            histories = new int[byteData.length / 2];
            for (int i = 0; i < histories.length; i++)
            {
                histories[i] = ((byteData[i * 2] << 8) & 0xFF00) | (byteData[i * 2 + 1] & 0xFF);
            }
        } catch (Exception e)
        {
            System.err.println(e);
        } finally
        {
            try
            {
            } catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    // Save history
    private void saveHistory()
    {
        byte[] byteData = new byte[histories.length * 2];
        for (int i = 0; i < histories.length; i++)
        {
            int x = histories[i];
            // 2 bytes = 65536 is more than dictionary lines
            byteData[i * 2] = (byte) ((x >> 8) & 0xFF);
            byteData[i * 2 + 1] = (byte) (x & 0xFF);
        }
        try
        {
            record.setRecord(dicIndex + 1, byteData, 0, byteData.length);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    // Reset history
    public void resetHistory()
    {
        try
        {
            record.closeRecordStore();
            RecordStore.deleteRecordStore(name);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
