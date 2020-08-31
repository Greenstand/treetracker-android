package org.greenstand.android.TreeTracker.utilities

import java.util.Queue
import java.util.concurrent.ConcurrentLinkedDeque

class BoundedConcurrentQueue<E>(var capacity: Int) : Queue<E> {

    private val deque = ConcurrentLinkedDeque<E>()

    override fun contains(element: E): Boolean {
        return deque.contains(element)
    }

    override fun addAll(elements: Collection<E>): Boolean {
        TODO("Not yet implemented")
    }

    override fun clear() {
        TODO("Not yet implemented")
    }

    override fun element(): E {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override fun remove(): E {
        TODO("Not yet implemented")
    }

    override val size: Int
        get() = TODO("Not yet implemented")

    override fun containsAll(elements: Collection<E>): Boolean {
        TODO("Not yet implemented")
    }

    override fun iterator(): MutableIterator<E> {
        TODO("Not yet implemented")
    }

    override fun remove(element: E): Boolean {
        TODO("Not yet implemented")
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        TODO("Not yet implemented")
    }

    override fun add(element: E): Boolean {
        TODO("Not yet implemented")
    }

    override fun offer(e: E): Boolean {
        TODO("Not yet implemented")
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        TODO("Not yet implemented")
    }

    override fun peek(): E {
        TODO("Not yet implemented")
    }

    override fun poll(): E {
        TODO("Not yet implemented")
    }
}
