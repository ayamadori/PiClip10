package ayamadori.piclip.dic;

public class CombinedDictionary
{

    private Words wordsDic;
    private Phrases phrasesDic;
    private boolean isYomi;

    public CombinedDictionary()
    {
        super();
        wordsDic = new Words();
        phrasesDic = new Phrases();
        isYomi = false;
    }

    public void search(String prefix, String yomi, String[] cand)
    {
        if (yomi == null || yomi.length() == 0) // there is no yomi
        {
            isYomi = false;
            cand[0] = "";
        }
        else
        {
            isYomi = true;
            cand[0] = yomi;
        }
        int i = (isYomi)? 1 : 0;
        phrasesDic.search(prefix, yomi, cand, i);
        wordsDic.search(prefix, yomi, cand, i + phrasesDic.getCandsSize());
    }

    public void learning(int candNumber)
    {
        if (isYomi && candNumber == 0)
        {
            return;
        }
        int i = (isYomi)? 1 : 0;
        if (candNumber - i < phrasesDic.getCandsSize())
        {
            phrasesDic.learning(candNumber - i);
        }
        else
        {
            wordsDic.learning(candNumber - phrasesDic.getCandsSize() - i);
        }
    }

    public void resetHistory()
    {
        wordsDic.resetHistory();
        phrasesDic.resetHistory();
    }

    public int searchMode()
    {
        int mode = wordsDic.searchMode();
        if (mode != phrasesDic.searchMode())
        {
            phrasesDic.setSearchMode(mode);
        }
        return wordsDic.searchMode();
    }

    public void setSearchMode(int mode)
    {
        wordsDic.setSearchMode(mode);
        phrasesDic.setSearchMode(mode);
    }

    public int getCandsSize()
    {
        int size = phrasesDic.getCandsSize() + wordsDic.getCandsSize();
        if(isYomi) size++;
        if (size > Dictionary.MAX_CANDIDATE)
        {
            size = Dictionary.MAX_CANDIDATE;
        }
        return size;
    }
}
