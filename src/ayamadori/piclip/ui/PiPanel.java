/*
 * PiPanel.java v1.0.0
 *
 * Created on 2018/1/1
 */
package ayamadori.piclip.ui;

import ayamadori.piclip.PiClip;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.midlet.MIDlet;
import ayamadori.piclip.dic.CombinedDictionary;
import ayamadori.piclip.dic.Dictionary;

/**
 * @author Ayamadori
 */
// 主クラス
public class PiPanel extends Canvas implements CommandListener
{   
    private final MIDlet midlet;
    
    // 各キーへの文字割り当て
    private static final String[] KANA_KEYS =
    {
        "\u308f\u3092\u3093\u30fc\u3001\u3002\uff01\uff1f\u3000",
        "\u3042\u3044\u3046\u3048\u304a", "\u304b\u304d\u304f\u3051\u3053", "\u3055\u3057\u3059\u305b\u305d",
        "\u305f\u3061\u3064\u3066\u3068", "\u306a\u306b\u306c\u306d\u306e", "\u306f\u3072\u3075\u3078\u307b",
        "\u307e\u307f\u3080\u3081\u3082", "\u3084\u3086\u3088", "\u3089\u308a\u308b\u308c\u308d",
    };
    private static final String[] ABC_KEYS =
    {
        "\u0020\n", ".@/!?(),-_:;'\"&", "abc", "def", "ghi", "jkl", "mno", "pqrs", "tuv", "wxyz"
    };
    private static final String[] NUM_KEYS =
    {
        "0", "1", "2", "3", "4", "5", "6", "7", "8", "9"
    };
    // 入力モード表示用文字列
    private static final String[] INPUT_MODE =
    {
        "\u6F22\u5B57", "abc", "123"
    };
    private static final int KEY_CLEAR = -8; // クリアキーのキーコード
    private static final long KEY_TIMEOUT = 1000; // キー入力タイムアウト時間(msec)
    private static final int OFFSET = 2; // 表示オフセット
    private static final int CANDIDATE_SPACE = 10;// 予測候補表示の間隔
    private static final int CANDIDATE_MAX_LINE = 3;// 予測候補表示行数
    
    // -----------------------------------------------------------------
    // ソフトキー
    private final Command CMD_SHARE; // Share Menuに送る
    private final Command CMD_CONV; // 平仮名-カタカナ，全角-半角の相互変換
    private final Command CMD_NEW; // 新規作成
    private final Command CMD_NORMAL; // 通常の予測検索
    private final Command CMD_FULL_MATCH; // 完全一致検索
    private final Command CMD_CLEAR_DIC; // Clear dictionary
    private final Command CMD_SELECT; // JP-8搭載ソニエリ端末など、中央ソフトキーを持つ端末に対応
    private final Command CMD_ABOUT;
    private final Command CMD_CLEAR; // 1文字削除
    // -----------------------------------------------------------------

    private int iFirstLine;// 本文の表示開始行番号
    private int iCandFirstLine;// 予測候補の表示開始行番号
    private StringBuffer sbPre;// 確定文字
    private StringBuffer sbCur;// 編集文字
    private int iPreIdx;// 確定文字中のキャレット位置
    private int iCurIdx;// 編集文字中のキャレット位置
    private String prefix;
    private int iScrWidth; // 画面幅
    private int iScrHeight; // 画面高さ
    private int iMaxLine; // 最大表示行数
    private Font font;// 標準フォント
    private int iFontHeight;// フォント高さ

    private String[] keys;
    private boolean bRepeatedFlag; // キー長押し用フラグ
    private long iKeyReleaseTime; // キーを離した時刻

    private int iInputMode;// 入力モード
    private int iKeyMajor;// どこのキーを押しているか？
    private int iKeyMinor;// 何回同じキーを押しているか？

    private CombinedDictionary dictionary;// 予測辞書
    private String[] cand;// 予測候補
    private int iCandNum;// 選択している予測候補の番号

    // -----------------------------------------------------------------------
    // コンストラクタ
    public PiPanel(MIDlet mid)
    {
        // 基本
        midlet = mid;
        iFirstLine = 0;
        iCandFirstLine = 0;
        dictionary = new CombinedDictionary();
        cand = new String[Dictionary.MAX_CANDIDATE];
        font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_LARGE);
        iFontHeight = font.getHeight();

        // 入力文字関連
        sbPre = new StringBuffer("");
        sbCur = new StringBuffer("");
        keys = KANA_KEYS;
        iPreIdx = 0;
        iCurIdx = 0;
        iInputMode = 0;
        iKeyMajor = -1;
        bRepeatedFlag = false;
        iKeyReleaseTime = 0;
        iScrWidth = getWidth();
        iScrHeight = getHeight();
        iMaxLine = iScrHeight / iFontHeight - 2 - CANDIDATE_MAX_LINE;
        prefix = "";

        // ----------------------------------------------------------------
        // ソフトキー取り付け
        CMD_SELECT = new Command("Select", Command.SCREEN, 1);
        CMD_CONV = new Command("Character conv.", Command.SCREEN, 2);
        CMD_NORMAL = new Command("Normal predict", Command.SCREEN, 3);
        CMD_FULL_MATCH = new Command("Full match", Command.SCREEN, 4);
        CMD_NEW = new Command("New", Command.SCREEN, 5);
        CMD_CLEAR_DIC = new Command("Clear dictionary (App exit)", Command.SCREEN, 6);
        CMD_SHARE = new Command("Share", Command.SCREEN, 7);
        CMD_ABOUT = new Command("About", Command.SCREEN, 8);
        CMD_CLEAR = new Command("Clear", Command.CANCEL, 1);

        addCommand(CMD_SELECT);
        addCommand(CMD_CONV);
        addCommand(CMD_FULL_MATCH);
        addCommand(CMD_NEW);
        addCommand(CMD_CLEAR_DIC);
        addCommand(CMD_SHARE);
        addCommand(CMD_ABOUT);
        addCommand(CMD_CLEAR);
        setCommandListener(this);
    }

    // -----------------------------------------------------------------------
    // 描画メソッド
    protected void paint(Graphics g)
    {
        // 文字表示に用いるフォントを読み込む
        font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_LARGE);
        g.setFont(font);
        
        // Background
        g.setColor(PiClip.COLOR_BACKGROUND);
        g.fillRect(0, 0, iScrWidth, iScrHeight);
        
        int x = OFFSET, y = iFontHeight;
        int caretX = x, caretY = y;
        int i = 0;
        char ch;
        int iStrLine = 0;
        boolean bReturn = false;// 改行フラグ

        // 本文を描画----------------------------------------------------------------
        while (i < sbPre.length() + sbCur.length())
        {
            g.setColor(PiClip.COLOR_FOREGROUND);
            font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_LARGE);
            
            // 文字を抽出
            if (i < iPreIdx)
            {
                ch = sbPre.charAt(i);
            } else if (i < iPreIdx + sbCur.length())
            {
                ch = sbCur.charAt(i - iPreIdx);
            } else
            {
                ch = sbPre.charAt(i - sbCur.length());
            }

            // 文字幅を計算
            int cWidth = font.charWidth(ch);

            // 画面の幅に収まらないか、1つ前に改行記号がでたとき改行
            if ((x + cWidth) > (iScrWidth - OFFSET) || bReturn)
            {
                bReturn = false;// 改行フラグ下げ
                iStrLine++;
                x = OFFSET;
                if (iStrLine > iFirstLine)
                {
                    y += iFontHeight;
                    if (iStrLine - iFirstLine > iMaxLine + 1)
                    {
                        break;
                    }
                } else if (iPreIdx + iCurIdx - 1 < i)// こうしないと一部うまくスクロールしない
                {
                    // 上スクロール(=表示開始行を1減らす)
                    iFirstLine--;
                    paint(g);
                    return;// これが無いとpaint()の残りの描画処理を行ってしまう？
                }
                if (iStrLine - iFirstLine > iMaxLine && iPreIdx + iCurIdx > i)
                {
                    // 下スクロール(=表示開始行を1増やす)
                    iFirstLine++;
                    paint(g);
                    return;// これが無いとpaint()の残りの描画処理を行ってしまう？
                }
            }

            // 文字を削除していくとうまくスクロールしないことがあるから、その対処
            if (i == sbPre.length() + sbCur.length() - 1 && iStrLine < iFirstLine)
            {
                // 上スクロール(=表示開始行を1減らす)
                iFirstLine--;
                paint(g);
                return;// これが無いとpaint()の残りの描画処理を行ってしまう？
            }

            // 文字列を描画
            if (iStrLine >= iFirstLine)
            {
                // 未確定文字列の背景
                if (i >= iPreIdx && i < iPreIdx + sbCur.length())
                {
                    g.setColor(PiClip.COLOR_HIGHLIGHTED_BACKGROUND);
                    g.fillRect(x, y, cWidth, iFontHeight);
                    g.setColor(PiClip.COLOR_HIGHLIGHTED_FOREGROUND);
                }
                // キャレット位置
                if (i == iPreIdx + iCurIdx - 1)
                {
                    if (ch == '\n')
                    {
                        caretX = OFFSET;
                        caretY = y + iFontHeight;
                        if (caretY + iFontHeight >= iScrHeight - iFontHeight * CANDIDATE_MAX_LINE)
                        {
                            // キャレットが画面からはみ出たら下スクロール(=表示開始行を1増やす)
                            iFirstLine++;
                            paint(g);
                            return;// これが無いとpaint()の残りの描画処理を行ってしまう？
                        }
                    } else
                    {
                        caretX = x + cWidth;
                        caretY = y;
                    }
                }

                // 文字を描画
                g.drawChar(ch, x, y + font.getBaselinePosition(), Graphics.BASELINE | Graphics.LEFT);
            }

            // 改行記号が出たら改行フラグを上げる
            if (ch == '\n')
            {
                bReturn = true;
            }

            // 文字幅を加算
            x += cWidth;
            i++;
        }

        // キャレット表示-----------------------------------------------------------
        g.setColor(PiClip.COLOR_FOREGROUND);
        g.drawLine(caretX, caretY, caretX, caretY + iFontHeight - 1);

        // 予測候補表示------------------------------------------------------------
        g.setColor(PiClip.COLOR_FOREGROUND);
        g.fillRect(0, iScrHeight - iFontHeight * CANDIDATE_MAX_LINE, iScrWidth, iFontHeight * CANDIDATE_MAX_LINE);

        int iCandLine = 0;
        i = 0;
        x = OFFSET;
        y = iScrHeight - iFontHeight * CANDIDATE_MAX_LINE;
        while (i < dictionary.getCandsSize())
        {
            g.setColor(PiClip.COLOR_BACKGROUND);
            int sWidth = font.stringWidth(cand[i]);

            // 画面の幅に収まらないとき改行。候補文字列が画面幅より長いときはそのまま表示
            if (x + sWidth > iScrWidth - OFFSET && sWidth <= iScrWidth - OFFSET)
            {
                iCandLine++;
                x = OFFSET;

                if (iCandLine > iCandFirstLine)
                {
                    y += iFontHeight;
                    if (iCandLine - iCandFirstLine > CANDIDATE_MAX_LINE)
                    {
                        break;
                    }
                } else if (iCandNum < i)
                {
                    // 上スクロール(=表示開始行を1減らす)
                    iCandFirstLine--;
                    paint(g);
                    return;// これが無いとpaint()の残りの描画処理を行ってしまう？
                }
                if (iCandLine - iCandFirstLine >= CANDIDATE_MAX_LINE && iCandNum >= i)
                {
                    // 下スクロール(=表示開始行を1増やす)
                    iCandFirstLine++;
                    paint(g);
                    return;// これが無いとpaint()の残りの描画処理を行ってしまう？
                }
            }

            if (iCandLine >= iCandFirstLine)
            {
                if (i == iCandNum)
                {
                    g.setColor(PiClip.COLOR_HIGHLIGHTED_BACKGROUND);
                    g.fillRect(x, y, sWidth, iFontHeight);
                    g.setColor(PiClip.COLOR_HIGHLIGHTED_FOREGROUND);
                }
                g.drawString(cand[i], x, y + font.getBaselinePosition(), Graphics.BASELINE | Graphics.LEFT);
            }
            x += sWidth + CANDIDATE_SPACE;
            i++;
        }

        // インジケータ------------------------------------------------------------------
        g.setColor(PiClip.COLOR_HIGHLIGHTED_BACKGROUND);
        font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
        g.setFont(font);

        // 入力モード
        g.drawString(INPUT_MODE[iInputMode], OFFSET, font.getBaselinePosition(), Graphics.BASELINE | Graphics.LEFT);

        // 文字数
        g.drawString(sbPre.length() + sbCur.length() + "", iScrWidth - OFFSET, font.getBaselinePosition(), Graphics.BASELINE | Graphics.RIGHT);

        // 矢印
        if (sbCur.length() > 0 || prefix.length() > 0)
        {
            if (iCandLine > iCandFirstLine + CANDIDATE_MAX_LINE - 1) drawDownAllow(g, font.getHeight()); // 下矢印
            if (iCandFirstLine > 0) drawUpAllow(g, font.getHeight());  // 上矢印
        }
        else
        {
            if (iStrLine > iFirstLine + iMaxLine) drawDownAllow(g, font.getHeight()); // 下矢印
            if (iFirstLine > 0) drawUpAllow(g, font.getHeight());  // 上矢印
        }
        
        font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_LARGE);
        g.setFont(font);
    }
    
    // 下矢印
    private void drawDownAllow(Graphics g, int height)
    {
        g.drawLine(iScrWidth / 2 - height * 3 / 10, height * 6 / 10, iScrWidth / 2, height * 9 / 10);
        g.drawLine(iScrWidth / 2 + height * 3 / 10, height * 6 / 10, iScrWidth / 2, height * 9 / 10);
    }

    // 上矢印
    private void drawUpAllow(Graphics g, int height)
    {
        g.drawLine(iScrWidth / 2 - height * 3 / 10, height * 4 / 10, iScrWidth / 2, height / 10);
        g.drawLine(iScrWidth / 2 + height * 3 / 10, height * 4 / 10, iScrWidth / 2, height / 10);
    }
    
    // -----------------------------------------------------------------------
    // キーイベント
    protected void keyPressed(int keyCode)
    {
        char chCur;

        if (KEY_NUM0 <= keyCode && keyCode <= KEY_NUM9)
        {
            bRepeatedFlag = true;// 最初キーが押されたときにフラグをあげる
            iCandNum = 0;
            iCandFirstLine = 0;
            // 前回のキーを離した時間より一定間隔を超えると仮確定
            if ((System.currentTimeMillis() - iKeyReleaseTime) > KEY_TIMEOUT)
            {
                iKeyMajor = -1;
            }
            // マルチタップ入力
            int index = keyCode - KEY_NUM0;
            if (index != iKeyMajor)
            {
                iKeyMinor = 0;
                iKeyMajor = index;
                chCur = keys[iKeyMajor].charAt(0);
                sbCur.insert(iCurIdx, chCur);
                iCurIdx++;
            }
            else
            {
                iKeyMinor++;
                if (iKeyMinor >= keys[iKeyMajor].length())
                {
                    iKeyMinor = 0;
                }
                chCur = keys[iKeyMajor].charAt(iKeyMinor);
                sbCur.setCharAt(iCurIdx - 1, chCur);
            }
            dictionary.search(prefix, sbCur.toString(), cand);
        }
        else if (keyCode == KEY_STAR)
        {
            iCandNum = 0;
            iCandFirstLine = 0;
            if (sbCur.length() == 0) return;
            iKeyMajor = -1;// 他のどれもが使ってない数字で逃げる

            char ch = sbCur.charAt(iCurIdx - 1);
            int kana = (ch >= '\u30a1' && ch <= '\u30fe') ? 96 : 0;// かな＞カナ変換

            // 大文字、小文字、濁音、半濁音、清音の相互変換
            if ((ch >= '\u3041' + kana && ch <= '\u3062' + kana)// 'あ'～'ぢ'
                    || (ch >= '\u3083' + kana && ch <= '\u3088' + kana))// 'ゃ'～'よ'
            {
                ch = (char) ((ch % 2 == 0) ? ch - 1 : ch + 1);
            }
            else if (ch == '\u3064' + kana || ch == '\u3065' + kana)// 'つ', 'づ'
            {
                ch--;
            }
            else if (ch == '\u3063' + kana)// 'っ'
            {
                ch += 2;
            }
            else if ((ch >= '\u3066' + kana && ch <= '\u3069' + kana)// 'て'～'ど'
                    || (ch >= '\u308e' + kana && ch <= '\u308f' + kana))// 'ゎ'～'わ'
            {
                ch = (char) ((ch % 2 == 0) ? ch + 1 : ch - 1);
            }
            else if (ch >= '\u306f' + kana && ch <= '\u307d' + kana)// 'は'～'ぽ'
            {
                ch = (char) ((ch % 3 == 2) ? ch - 2 : ch + 1);
            }
            else if (ch >= 'a' && ch <= 'z')
            {
                ch -= 32;
            }
            else if (ch >= 'A' && ch <= 'Z')
            {
                ch += 32;
            }
            sbCur.setCharAt(iCurIdx - 1, ch);
            dictionary.search(prefix, sbCur.toString(), cand);
        }
        else if (keyCode == KEY_POUND)
        {
            if (iKeyMajor >= 0 && iKeyMajor <= 9)
            {
                pressFire();
            }
            iKeyMajor = -1;
            // 入力モード切替
            iInputMode = (iInputMode < 2) ? iInputMode + 1 : 0;
            switch (iInputMode)
            {
                case 1:
                    keys = ABC_KEYS;
                    break;
                case 2:
                    keys = NUM_KEYS;
                    break;
                default:
                    keys = KANA_KEYS;
                    break;
            }
        }
        else if (keyCode == KEY_CLEAR)
        {
            iKeyMajor = -1;
            deleteChar();
            // 完全一致モードなら通常予測に戻す
            if (dictionary.searchMode() == Dictionary.PREDICT_FULL_MATCH)
            {
                dictionary.setSearchMode(Dictionary.PREDICT_NORMAL);
                removeCommand(CMD_NORMAL);
                addCommand(CMD_FULL_MATCH);
            }
            dictionary.search(prefix, sbCur.toString(), cand);
            // 予測候補は最初に戻す
            iCandNum = 0;
            iCandFirstLine = 0;
            if(sbCur.length() == 0 && dictionary.getCandsSize() == 0) prefix = "";
        }
        else
        {
            iKeyMajor = -1;
            switch (getGameAction(keyCode))
            {
                case RIGHT:
                    if (prefix.length() > 0 || sbCur.length() > 0)
                    {
                        // 予測候補を1つ右に移動
                        iCandNum++;
                        if (iCandNum >= dictionary.getCandsSize())
                        {
                            iCandNum = 0;
                            iCandFirstLine = 0;
                        }
                    }
                    else if (iPreIdx < sbPre.length())
                    {
                        // キャレット右移動
                        iPreIdx++;
                    }
                    else if (iPreIdx == sbPre.length())
                    {
                        // 空白を追加
                        if (iInputMode == 1 || iInputMode == 2)
                        {
                            sbPre.append(' ');
                        } else
                        {
                            sbPre.append('\u3000');
                        }
                        iPreIdx++;
                    }
                    break;

                case LEFT:
                    if (prefix.length() > 0 || sbCur.length() > 0)
                    {
                        // 予測候補を1つ左に移動
                        iCandNum--;
                        if (iCandNum < 0)
                        {
                            iCandNum = dictionary.getCandsSize() - 1;
                        }
                    }
                    else if (iPreIdx > 0)
                    {
                        // キャレット左移動
                        iPreIdx--;
                    }
                    break;

                case UP:
                    int wU = iScrWidth - OFFSET * 2;

                    if (prefix.length() > 0 || sbCur.length() > 0)
                    {
                        // 予測候補の上移動。改行時の空白は無視されるから、その分を考慮して比較
                        while (wU + CANDIDATE_SPACE >= 0)
                        {
                            if (iCandNum < 0)
                            {
                                iCandNum = dictionary.getCandsSize() - 2;
                                break;
                            }
                            wU -= font.stringWidth(cand[iCandNum]) + CANDIDATE_SPACE;
                            iCandNum--;
                        }
                        iCandNum++;
                    } else
                    {
                        // キャレット上移動
                        iPreIdx--;
                        while (wU >= 0)
                        {
                            if (iPreIdx < 0)
                            {
                                iPreIdx = -2;
                                break;
                            }
                            char ch = sbPre.charAt(iPreIdx);
                            if (ch == '\n')// 途中に改行があればキャレットをそこに移動
                            {
                                iPreIdx -= 2;
                                break;
                            }
                            wU -= font.charWidth(ch);
                            iPreIdx--;
                        }
                        iPreIdx += 2;
                    }
                    break;

                case DOWN:
                    int wD = iScrWidth - OFFSET * 2;

                    if (prefix.length() > 0 || sbCur.length() > 0)
                    {
                        if (iCandNum == dictionary.getCandsSize() - 1)
                        {
                            iCandNum = 0;
                            iCandFirstLine = 0;
                            break;
                        }
                        // 予測候補の下移動。改行時の空白は無視されるから、その分を考慮して比較
                        while (wD + CANDIDATE_SPACE >= 0)
                        {
                            if (iCandNum >= dictionary.getCandsSize())
                            {
                                iCandNum = dictionary.getCandsSize();
                                break;
                            }
                            wD -= font.stringWidth(cand[iCandNum]) + CANDIDATE_SPACE;
                            iCandNum++;
                        }
                        iCandNum--;
                    } else
                    {
                        // 現在改行位置にいればその次の文字に移動
                        if (iPreIdx < sbPre.length() && sbPre.charAt(iPreIdx) == '\n')
                        {
                            iPreIdx++;
                            break;
                        }
                        // 現在文字列の終端にいたら改行を追加
                        if (iPreIdx == sbPre.length())
                        {
                            sbPre.insert(iPreIdx, "\n");
                            iPreIdx++;
                            break;
                        }
                        // キャレット下移動
                        while (wD >= 0)
                        {
                            if (iPreIdx >= sbPre.length())
                            {
                                iPreIdx = sbPre.length() + 1;
                                break;
                            }
                            char ch = sbPre.charAt(iPreIdx);
                            if (ch == '\n')// 途中に改行があればキャレットをそこに移動
                            {
                                iPreIdx++;
                                break;
                            }
                            wD -= font.charWidth(ch);
                            iPreIdx++;
                        }
                        iPreIdx--;
                    }
                    break;

                case FIRE:
                    pressFire();
                    break;

            }
        }
        // 画面を更新
        repaint();

    }

    // ---------------------------------------------------------------------
    // キーを離すタイミングを記憶
    protected void keyReleased(int keyCode)
    {
        if (KEY_NUM0 <= keyCode && keyCode <= KEY_NUM9)
        {
            iKeyReleaseTime = System.currentTimeMillis();
        }
    }

    // ---------------------------------------------------------------------
    // キー長押しで数字確定入力(端末依存)
    protected void keyRepeated(int keyCode)
    {
        if (KEY_NUM0 <= keyCode && keyCode <= KEY_NUM9 && sbCur.length() == 1 && bRepeatedFlag)
        {
            bRepeatedFlag = false;// いったんketRepeatedに入ったらフラグを下げてrepaint()ループを防止
            iCandNum = 0;
            iCandFirstLine = 0;
            char chCur;
            int index = keyCode - KEY_NUM0;
            iKeyMinor = 0;
            iKeyMajor = -1;
            chCur = NUM_KEYS[index].charAt(0);
            sbPre.insert(iPreIdx, chCur);
            sbCur.deleteCharAt(iCurIdx - 1);
            iCurIdx = 0;
            iPreIdx++;
            prefix = "";
            dictionary.search(prefix, sbCur.toString(), cand);
            repaint();
        }
    }

    // ------------------------------------------------------------------------
    // かなカナ、大文字小文字相互変換
    private char convertChar(char ch)
    {
        if (ch >= '\u3041' && ch <= '\u3093')
        {
            ch += 96; // かな＞カナ変換
        }
        else if (ch >= '\u30a1' && ch <= '\u30f3')
        {
            ch -= 96; // カナ＞かな変換
        }
        else if (ch >= 'A' && ch <= 'Z')
        {
            ch += 0x0020; // 大文字＞小文字変換
        }
        else if (ch >= 'a' && ch <= 'z')
        {
            ch -= 0x0020; // 小文字＞大文字変換
        }
        return ch;
    }

    // ------------------------------------------------------------------------
    // ソフトキー操作
    public void commandAction(Command c, Displayable d)
    {
        if (c == CMD_SELECT)
        {
            // キーコードを変更
            iKeyMajor = -1;
            // 決定キーを押したときの動作をエミュレート
            pressFire();
        } else if (c == CMD_CONV)
        {
            // 入力文字列のかなカナ、全半角相互変換
            if (sbCur.length() > 0)
            {
                for (int i = sbCur.length() - 1; i >= 0; i--)
                {
                    sbCur.setCharAt(i, convertChar(sbCur.charAt(i)));
                }
                dictionary.search(prefix, sbCur.toString(), cand);
                repaint();
            }
        }
        else if (c == CMD_FULL_MATCH)
        {
            dictionary.setSearchMode(Dictionary.PREDICT_FULL_MATCH);
            dictionary.search(prefix, sbCur.toString(), cand);
            // 予測候補は最初に戻す
            iCandNum = 0;
            iCandFirstLine = 0;
            repaint();
            removeCommand(CMD_FULL_MATCH);
            addCommand(CMD_NORMAL);
        }
        else if (c == CMD_NORMAL)
        {
            dictionary.setSearchMode(Dictionary.PREDICT_NORMAL);
            dictionary.search(prefix, sbCur.toString(), cand);
            // 予測候補は最初に戻す
            iCandNum = 0;
            iCandFirstLine = 0;
            repaint();
            removeCommand(CMD_NORMAL);
            addCommand(CMD_FULL_MATCH);
        }
        else if (c == CMD_NEW)
        {
            newText();
            // 完全一致モードなら通常予測に戻す
            if (dictionary.searchMode() == Dictionary.PREDICT_FULL_MATCH)
            {
                dictionary.setSearchMode(Dictionary.PREDICT_NORMAL);
                removeCommand(CMD_NORMAL);
                addCommand(CMD_FULL_MATCH);
            }
            dictionary.search(prefix, sbCur.toString(), cand);
            repaint();
        }
        else if (c == CMD_CLEAR)
        {
            // クリアキーを押したときの動作をエミュレート
            keyPressed(KEY_CLEAR);
        }
        else if (c == CMD_CLEAR_DIC)
        {
            // Clear dictionary
            dictionary.resetHistory();
            // App exit
            midlet.notifyDestroyed();
        }
        else if (c == CMD_SHARE)
        {
            if (sbCur.length() > 0)
            {
                pressFire();
            }
            ShareMenu share = new ShareMenu(midlet, sbPre.toString());
            share.showMenu(this);
        }
        else if (c == CMD_ABOUT)
        {
            new About(midlet, this);
        }
        // 画面を更新
        repaint();
    }

    // 1文字削除
    private void deleteChar()
    {
        if (prefix.length() == 0 && sbCur.length() == 0 && iPreIdx > 0)
        {
            iPreIdx--;
            sbPre.deleteCharAt(iPreIdx);
        }
        else if (sbCur.length() > 0)
        {
            if (iCurIdx > 0)
            {
                iCurIdx--;
                sbCur.deleteCharAt(iCurIdx);
            }
        }
        else if (prefix.length() > 0)
        {
            prefix = "";
        }
    }

    // 新規作成
    private void newText()
    {
        sbPre.delete(0, sbPre.length());
        sbCur.delete(0, sbCur.length());
        prefix = "";
        iKeyMajor = -1;
        iInputMode = 0;
        iPreIdx = 0;
        iCurIdx = 0;
        iFirstLine = 0;
        // 予測候補は最初に戻す
        iCandNum = 0;
        iCandFirstLine = 0;
        // 完全一致モードなら通常予測に戻す
        if (dictionary.searchMode() == Dictionary.PREDICT_FULL_MATCH)
        {
            dictionary.setSearchMode(Dictionary.PREDICT_NORMAL);
            removeCommand(CMD_NORMAL);
            addCommand(CMD_FULL_MATCH);
        }
        dictionary.search(prefix, sbCur.toString(), cand);
    }

    // 決定キーを押したときの動作
    private void pressFire()
    {
        if (cand[iCandNum] == null) return;
        // 文字列を確定
        sbPre.insert(iPreIdx, cand[iCandNum]);
        iPreIdx += cand[iCandNum].length();
        // 学習
        dictionary.learning(iCandNum);
        prefix = cand[iCandNum];
        sbCur.delete(0, sbCur.length());
        iCurIdx = 0;
        iCandNum = 0;
        iCandFirstLine = 0;
        // 完全一致モードなら通常予測に戻す
        if (dictionary.searchMode() == Dictionary.PREDICT_FULL_MATCH)
        {
            dictionary.setSearchMode(Dictionary.PREDICT_NORMAL);
            removeCommand(CMD_NORMAL);
            addCommand(CMD_FULL_MATCH);
        }
        dictionary.search(prefix, sbCur.toString(), cand);
        if(sbCur.length() == 0 && dictionary.getCandsSize() == 0) prefix = "";
    }

    // 画面サイズが変わったら画面高さ、幅を再取得して再描画(702NKなどでの全画面表示のバグ対策)
    protected void sizeChanged(int _w, int _h)
    {
        iScrWidth = _w;
        iScrHeight = _h;
        iMaxLine = iScrHeight / iFontHeight - 2 - CANDIDATE_MAX_LINE;// 何行"目"まで表示できるか？

        // どうせ描画をループさせるわけじゃないから、ここで再描画しておけばいいのでは？
        repaint();
    }
}
