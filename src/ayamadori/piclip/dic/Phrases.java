package ayamadori.piclip.dic;

public class Phrases extends Dictionary
{

    public Phrases()
    {
        super("phrases");
    }

    public void search(String prefix, String yomi, String[] cand, int start)
    {
        // No include original word in Candidates
        candLength = 0;
        if (prefix == null || prefix.length() == 0) return;

        // load dictionary
        loadDic(prefix.charAt(0) % SPLIT);
        if (dicData.length() < prefix.length()) return;
        
        // 検索が遅いので別の方法を試してみる(各行頭から線形検索)
        int lineNum = 0;
        int index = dicLine[lineNum];
        boolean addCand;
        while (candLength < cand.length - start && index < dicData.length())
        {
            // 一応先頭から全ての文字を比較する
            addCand = true;
            for (int i = 0; i < prefix.length(); i++)
            {
                if (prefix.charAt(i) != dicData.charAt(index + i))
                {
                    addCand = false;
                    break;
                }
            }
            // プレフィックスは常に完全一致
            index += prefix.length();
            if (index < dicData.length() && dicData.charAt(index) != ' ')
            {
                addCand = false;
            }
            if (yomi.length() > 0 && addCand)
            {
                int last;
                if (lineNum < dicLine.length - 1 && dicLine[lineNum + 1] != 0)
                {
                    last = dicLine[lineNum + 1];
                } else
                {
                    last = dicData.length() - 1;
                }
                int st = lastIndexOf(dicData, ' ', last) + 1;
                for (int i = 0; i < yomi.length(); i++)
                {
                    if (toHiraSei(yomi.charAt(i)) != dicData.charAt(st + i))
                    {
                        addCand = false;
                        break;
                    }
                }
                // 完全一致検索
                if (searchMode == PREDICT_FULL_MATCH && dicData.charAt(st + yomi.length()) != '\n')
                {
                    addCand = false;
                }
            }
            if (addCand)
            {
                int st = indexOf(dicData, ' ', index) + 1;
                int en = indexOf(dicData, ' ', st);
                char[] tempChars = new char[en - st];
                dicData.getChars(st, en, tempChars, 0);
                String temp = new String(tempChars);
                // 重複候補は除く
                if (!temp.equals(yomi))
                {
                    // No include original word
                    cand[start + candLength] = temp;
                    // But DicLine should start from zero index
                    candDicLine[candLength] = lineNum;
                    candLength++;
                }
            }
            lineNum++;
            if ((index = dicLine[lineNum]) == 0) return;
        }
    }
}
