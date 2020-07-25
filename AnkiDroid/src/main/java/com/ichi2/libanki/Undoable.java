package com.ichi2.libanki;

import android.content.res.Resources;

import com.ichi2.anki.CardUtils;
import com.ichi2.libanki.Collection.DismissType;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

import static com.ichi2.libanki.Collection.DismissType.*;

public abstract class Undoable {
    private final DismissType mDt;
    public static final long MULTI_CARD = -1L;
    public static final long NO_REVIEW = 0L;

    /**
     * For all descendants, we assume that a card/note/object passed as argument is never going to be changed again.
     * It's the caller reponsability to clone the object if necessary.*/
    public Undoable(DismissType dt) {
        mDt = dt;
    }

    public String name(Resources res) {
        return res.getString(mDt.undoNameId);
    }

    public DismissType getDismissType() {
        return mDt;
    }

    /**
     * Return MULTI_CARD when no other action is needed, e.g. for multi card action
     * Return NO_REVIEW when we just need to reset the collection
     * Returned positive integers are card id. Those ids is the card that was discarded and that may be sent back to the reviewer.*/
    public abstract long undo(Collection col);



    public static class UndoableFlushAll extends Undoable {
        private final List<Card> mCards;
        private final long mCid;
        public UndoableFlushAll(DismissType dt, List<Card> cards, long cid) {
            super(dt);
            mCards = cards;
            mCid = cid;
        }

        public long undo(Collection col) {
            Timber.i("Undo: Bury Card");
            for (Card cc : mCards) {
                cc.flush(false);
            }
            return mCid;
        }
    }

}