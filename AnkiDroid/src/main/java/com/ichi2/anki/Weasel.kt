package com.ichi2.anki
import com.ichi2.libanki.Card
import com.ichi2.libanki.Collection
import com.ichi2.libanki.Utils
import timber.log.Timber
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

fun quux(col: Collection): Sequence<String> {
    val cursor = col.db.database.query(
        """
        SELECT cards.id FROM cards, notes 
        WHERE cards.nid = notes.id
        AND notes.flds like "%a%"
        """
    )

    return sequence {
        while (cursor.moveToNext()) {
            val id = cursor.getLong(0)
            val card = Card(col, id)
            card.note()

            yield("foo")
        }
    }
}

fun foo(col: Collection): Sequence<String> {
    val cursor = col.db.database.query(
        """
        SELECT cards.id FROM cards, notes 
        WHERE cards.nid = notes.id
        AND notes.flds like "%a%"
        """
    )

    return sequence {
        while (cursor.moveToNext()) {
            val id = cursor.getLong(0)
            val card = Card(col, id)
            yield(card.qSimple())
        }
    }
}

fun bar(col: Collection): Sequence<String> {
    val cursor = col.db.database.query(
        """
        SELECT cards.id, notes.mid, cards.did, cards.ord, notes.flds, notes.flags, notes.tags 
        FROM cards, notes 
        WHERE cards.nid = notes.id
        AND notes.flds like "%a%"
        """
    )

    return sequence {
        while (cursor.moveToNext()) {
            val cid = cursor.getLong(0)
            val mid = cursor.getLong(1)
            val did = cursor.getLong(2)
            val ord = cursor.getInt(3)
            val fields = cursor.getString(4)
            val flags = cursor.getInt(5)
            val tags = cursor.getString(6)

            val model = col.models.get(mid)!!
            val splitFields = Utils.splitFields(fields)
            yield(col._renderQA(cid, model, did, ord, tags, splitFields, flags)["q"]!!)
        }
    }
}

@Suppress("UNUSED_VARIABLE")
fun baz(col: Collection): Sequence<String> {
    val cursor = col.db.database.query(
        """
        SELECT cards.id, notes.mid, cards.did, cards.ord, notes.flds, notes.flags, notes.tags 
        FROM cards, notes 
        WHERE cards.nid = notes.id
        AND notes.flds like "%a%"
        """
    )

    return sequence {
        while (cursor.moveToNext()) {
            val cid = cursor.getLong(0)
            val mid = cursor.getLong(1)
            val did = cursor.getLong(2)
            val ord = cursor.getInt(3)
            val fields = cursor.getString(4)
            val flags = cursor.getInt(5)
            val tags = cursor.getString(6)

            yield("foo")
        }
    }
}

@OptIn(ExperimentalTime::class)
fun runQuery(col: Collection, which: Int) {
    val questions = when (which) {
        0 -> quux(col)
        1 -> foo(col)
        2 -> bar(col)
        else -> baz(col)
    }

    val size: Int

    val totalTime = measureTime {
        size = questions.toList().size
    }

    Timber.wtf("runQuery #$which: found $size questions in $totalTime")
}
