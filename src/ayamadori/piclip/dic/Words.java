package ayamadori.piclip.dic;

public class Words extends Dictionary
{

    public Words()
    {
        super("words");
    }

    public void search(String prefix, String yomi, String[] cand, int start)
    {
        // No include original word in Candidates
        candLength = 0;
        if (yomi == null || yomi.length() == 0) return;

        // load dictionary
        loadDic(toHiraSei(yomi.charAt(0)) % SPLIT);
        if (dicData.length() == 0) return;
        
        // 検索が遅いので別の方法を試してみる(各行頭から線形検索)
        int lineNum = 0;
        int index = dicLine[lineNum];
        boolean addCand;
        while (candLength < cand.length - start && index < dicData.length())
        {
            // 文字列を比較する
            addCand = true;
            for (int i = 0; i < yomi.length(); i++)
            {
                if (toHiraSei(yomi.charAt(i)) != dicData.charAt(index + i))
                {
                    addCand = false;
                    break;
                }
            }
            // 完全一致検索
            index += yomi.length();
            if (searchMode == PREDICT_FULL_MATCH && index < dicData.length() && dicData.charAt(index) != ' ')
            {
                addCand = false;
            }
            if (addCand)
            {
                int st = indexOf(dicData, ' ', index) + 1;
                int en = indexOf(dicData, '\n', st);
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
