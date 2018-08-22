package org.greenstand.android.TreeTracker.database

class Tree(var distance: Int, var restOfData: Array<Any>?) : Comparable<Tree> {

    override fun compareTo(tree: Tree): Int {
        return this.distance - tree.distance
    }

}
