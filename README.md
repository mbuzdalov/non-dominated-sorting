# non-dominated-sorting
This repo contains implementations of algorithms for non-dominated sorting and a benchmarking suite.

[![Build Status](https://travis-ci.org/mbuzdalov/non-dominated-sorting.png)](http://travis-ci.org/mbuzdalov/non-dominated-sorting)

## Algorithms

The following algorithms are currently implemented:

* Fast Non-Dominated Sorting
* Deductive Sort
* Corner Sort
* Dominance Tree
* ENS
* Best Order Sort
* Jensen-Fortin-Buzdalov divide-and-conquer method

### Fast Non-Dominated Sorting

```
@article{ nsga-ii,
    author      = {Kalyanmoy Deb and Amrit Pratap and Sameer Agarwal and T. Meyarivan},
    title       = {A Fast and Elitist Multi-Objective Genetic Algorithm: {NSGA}-{II}},
    journal     = {IEEE Transactions on Evolutionary Computation},
    year        = {2002},
    volume      = {6},
    number      = {2},
    pages       = {182-197},
    publisher   = {IEEE Press},
    langid      = {english}
}
```

How to get an instance:

* [FastNonDominatedSorting](
https://github.com/mbuzdalov/non-dominated-sorting/blob/master/implementations/src/main/java/ru/ifmo/nds/FastNonDominatedSorting.java
)`.getOriginalVersion()` -- returns a close-to-original implementation with O(MN^2) worst-case running time and O(N^2) memory. Every possible objective comparison is done exactly once.
* [FastNonDominatedSorting](
https://github.com/mbuzdalov/non-dominated-sorting/blob/master/implementations/src/main/java/ru/ifmo/nds/FastNonDominatedSorting.java
)`.getLinearMemoryImplementation()` -- returns an implementation with O(MN^2) worst-case running time and O(N) memory. Every possible objective comparison is done at most twice. This is either slightly slower or slightly faster than the original implementation, but saves a lot of memory.

### Deductive Sort

```
@article{ deductive-and-climbing-sort,
    author      = {Kent McClymont and Ed Keedwell},
    title       = {Deductive Sort and Climbing Sort: New Methods for Non-Dominated Sorting}
    journal     = {Evolutionary Computation},
    year        = {2012},
    volume      = {20},
    number      = {1},
    pages       = {1-26},
    publisher   = {MIT Press},
    langid      = {english}
}
```

How to get an instance:

* [DeductiveSort](
https://github.com/mbuzdalov/non-dominated-sorting/blob/master/implementations/src/main/java/ru/ifmo/nds/DeductiveSort.java
)`.getInstance()` -- returns an instance of the Deductive Sort algorithm. The worst-case running time complexity (contrary to what claimed in the paper) is O(MN^3), the required memory is O(N). It often works 2-3 times faster than Fast Non-Dominated Sorting.  

### Corner Sort

```
@article{ corner-sort,
    author      = {Handing Wang and Xin Yao},
    title       = {Corner Sort for Pareto-Based Many-Objective Optimization},
    journal     = {IEEE Transactions on Cybernetics},
    year        = {2013},
    volume      = {44},
    number      = {1},
    pages       = {92-102},
    publisher   = {IEEE},
    langid      = {english}
}
```

How to get an instance:

* [CornerSort](
https://github.com/mbuzdalov/non-dominated-sorting/blob/master/implementations/src/main/java/ru/ifmo/nds/CornerSort.java
)`.getInstance()` -- returns an instance of the Corner Sort algorithm. The worst-case running time complexity is O(MN^2), the required memory is O(N). This algorithm seems to be among the best algorithms published before 2015 when working with M around 10 or higher.

### Dominance Tree

```
@article{ dominance-tree,
    author      = {Hongbing Fang and Qian Wang and Yi-Cheng Tu and Mark F. Horstemeyer},
    title       = {An Efficient Non-dominated Sorting Method for Evolutionary Algorithms},
    journal     = {Evolutionary Computation},
    year        = {2008},
    volume      = {16},
    number      = {3},
    pages       = {355-384},
    publisher   = {MIT Press},
    langid      = {english}
}
```

How to get an instance:
* [DominanceTree](
https://github.com/mbuzdalov/non-dominated-sorting/blob/master/implementations/src/main/java/ru/ifmo/nds/DominanceTree.java
)`.getNoPresortInsertion(boolean useRecursiveMerge)` -- returns an instance which uses neither preliminary lexicographical sorting of points, nor so-called "delayed insertion". If `useRecursiveMerge` is `true`, then merge operations on multiple trees are done as in mergesort, which appears to influence running times positively.
* [DominanceTree](
https://github.com/mbuzdalov/non-dominated-sorting/blob/master/implementations/src/main/java/ru/ifmo/nds/DominanceTree.java
)`.getPresortInsertion(boolean useRecursiveMerge, InsertionOption insertionOption)` -- returns an instance which uses preliminary lexicographical sorting of points. The `InsertionOption` argument controls "delayed insertion": if it is `NO_DELAYED_INSERTION`, no delayed insertion is performed; if it is `DELAYED_INSERTION_SEQUENTIAL_CONCATENATION`, sequential concatenation is used for delayed insertion, if it is `DELAYED_INSERTION_RECURSIVE_CONCATENATION`, then mergesort-like concatenation is used for delayed insertion.

Currently it appears that the `getPresortInsertion(true, DELAYED_INSERTION_RECURSIVE_CONCATENATION)` performs slightly better than other choices from this algorithm,
apart from smallest numbers of points (say, 10).

All versions require O(N) memory. They seem to have O(MN^2) worst-case running time complexity, although the proof in the paper is not strict enough to say it confidently.
Presort versions are courtesy of this project since this possibility is not discussed in the original paper.
Delayed insertion is currently not implemented for the no-presort version, as the paper did not make it clear, and I had troubles understanding how to implement it.  

### ENS

```
@article{ ens,
    author      = {Xingyi Zhang and Ye Tian and Ran Cheng and Yaochu Jin},
    title       = {An Efficient Approach to Nondominated Sorting for Evolutionary Multiobjective Optimization},
    journal     = {IEEE Transactions on Evolutionary Computation},
    year        = {2015},
    volume      = {19},
    number      = {2},
    pages       = {201-213},
    publisher   = {IEEE},
    langid      = {english}
}
```

How to get an instance:
* [ENS](
https://github.com/mbuzdalov/non-dominated-sorting/blob/master/implementations/src/main/java/ru/ifmo/nds/ENS.java
)`.getENS_SS()` -- returns an instance of ENS with sequential insertion.
* [ENS](
https://github.com/mbuzdalov/non-dominated-sorting/blob/master/implementations/src/main/java/ru/ifmo/nds/ENS.java
)`.getENS_BS()` -- returns an instance of ENS with binary search insertion.

Both implementations require O(N) memory and have worst-case running time complexity O(MN^2).
The `ENS_SS` version generally performs faster (up to 2 maybe times), except for occasional M=2.
Currently these algorithms are slowest when the number of fronts is about 1.
However, `ENS_SS` was the fastest algorithm on high-dimensional uniform inputs among all algorithms published on or before 2015.

### Best Order Sort

```
@inproceedings{ best-order-sort-gecco,
    author      = {Proteek Chandan Roy and Md. Monirul Islam and Kalyanmoy Deb},
    title       = {Best Order Sort: A New Algorithm to Non-dominated Sorting for Evolutionary Multi-objective Optimization},
    booktitle   = {Proceedings of the Genetic and Evolutionary Computation Conference Companion},
    year        = {2016},
    pages       = {1113-1120},
    publisher   = {ACM},
    langid      = {english}
}
```

How to get an instance:
* [BestOrderSort](
https://github.com/mbuzdalov/non-dominated-sorting/blob/master/implementations/src/main/java/ru/ifmo/nds/BestOrderSort.java
)`.getProteekImplementation()` -- returns an implementation which is directly based on [Proteek's code](https://github.com/Proteek/Best-Order-Sort) with necessary interface adaptations and removing the special case for M=2.
* [BestOrderSort](
https://github.com/mbuzdalov/non-dominated-sorting/blob/master/implementations/src/main/java/ru/ifmo/nds/BestOrderSort.java
)`.getImprovedImplementation()` -- returns the same ideas reimplemented from scratch (with some more care about getting rid of objectives which are not necessary), which brought a steady 1.5x speedup.

Both implementations require O(MN) memory and have worst-case running time complexity O(MN^2). However, these algorithms are **very** fast, especially in medium dimensions, small numbers of fronts and less than 10^5 points.

### Jensen-Fortin-Buzdalov

```
@article{ jensen,
    author      = {Mikkel T. Jensen},
    title       = {Reducing the Run-time Complexity of Multiobjective {EA}s: The {NSGA}-{II} and Other Algorithms},
    journal     = {IEEE Transactions on Evolutionary Computation},
    volume      = {7},
    number      = {5},
    year        = {2003},
    pages       = {503-515},
    publisher   = {IEEE Press},
    langid      = {english}
}

@inproceedings{ fortin,
    author      = {Fortin, F{\'e}lix-Antoine and Grenier, Simon and Parizeau, Marc},
    title       = {Generalizing the Improved Run-time Complexity Algorithm for Non-dominated Sorting},
    booktitle   = {Proceedings of Genetic and Evolutionary Computation Conference},
    year        = {2013},
    pages       = {615-622},
    publisher   = {ACM},
    langid      = {english}
}

@incollection{ buzdalov,
    author      = {Maxim Buzdalov and Anatoly Shalyto},
    title       = {A Provably Asymptotically Fast Version of the Generalized Jensen Algorithm for Non-Dominated Sorting},
    booktitle   = {Parallel Problem Solving from Nature -- {PPSN} {XIII}},
    series      = {Lecture Notes in Computer Science},
    number      = {8672},
    year        = {2014},
    pages       = {528-537},
    publisher   = {Springer},
    langid      = {english}
}

```

How to get an instance:

* [JensenFortinBuzdalov](
https://github.com/mbuzdalov/non-dominated-sorting/blob/master/implementations/src/main/java/ru/ifmo/nds/JensenFortinBuzdalov.java
)`.getRedBlackTreeSweepImplementation()` -- returns an implementation of the principles from all three works, which uses red-black trees in sweep-line subroutines. The memory complexity is O(MN), the worst-case running time complexity is O(min(MN^2), N (log N)^(M - 1)).
* [JensenFortinBuzdalov](
https://github.com/mbuzdalov/non-dominated-sorting/blob/master/implementations/src/main/java/ru/ifmo/nds/JensenFortinBuzdalov.java
)`.getFenwickSweepImplementation()` -- same as above, but with Fenwick tree for sweep-lines. This is always slower than the previous one.
* [JensenFortinBuzdalov](
https://github.com/mbuzdalov/non-dominated-sorting/blob/master/implementations/src/main/java/ru/ifmo/nds/JensenFortinBuzdalov.java
)`.getRedBlackTreeSweepHybridImplementation()` -- returns a hybrid implementation, which works as the red-black tree implementation, but switches to linear-memory fast non-dominated sorting on small enough subproblems. This version works much faster than the simple red-black-tree implementation at M>2, and is currently the fastest available algorithm. The memory requirements are O(MN), and no meaningful upper bounds are known other than O(min(MN^2), N (log N)^(M - 1)).

