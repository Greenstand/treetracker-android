package org.greenstand.android.TreeTracker.database

class Tree(var distance: Int, var restOfData: Array<Any>?) : Comparable<Tree> {

    override fun compareTo(other: Tree): Int {
        return this.distance - other.distance
    }

}
