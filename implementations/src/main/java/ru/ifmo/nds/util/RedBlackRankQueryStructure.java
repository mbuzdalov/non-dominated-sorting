package ru.ifmo.nds.util;

/**
 * This is an implementation of the rank query structure using a tailored version of the Red-Black Tree
 * from the Scala collection library.
 *
 * @author Rui Gonçalves
 * @author Maxim Buzdalov
 */
public final class RedBlackRankQueryStructure extends RankQueryStructureDouble {
    private final Node[] allNodes;

    public RedBlackRankQueryStructure(int maximumPoints) {
        allNodes = new Node[maximumPoints];
        for (int i = 0; i < maximumPoints; ++i) {
            allNodes[i] = new Node();
            allNodes[i].index = i;
        }
    }

    @Override
    public String getName() {
        return "Red-Black Tree";
    }

    @Override
    public int maximumPoints() {
        return allNodes.length;
    }

    @Override
    public boolean supportsMultipleThreads() {
        return true;
    }

    @Override
    public RangeHandle createHandle(int storageStart, int from, int until, int[] indices, double[] values) {
        return new RangeHandleImpl(allNodes, storageStart);
    }

    private static class Node {
        double key;
        int value;
        int index;
        boolean red;
        Node left, right, parent;
    }

    private static final class RangeHandleImpl extends RankQueryStructureDouble.RangeHandle {
        private final Node[] allNodes;
        private Node root = null;
        private int size = 0;
        private final int offset;

        private RangeHandleImpl(Node[] allNodes, int storageStart) {
            this.allNodes = allNodes;
            this.offset = storageStart;
        }

        @Override
        public RangeHandle put(double key, int value) {
            Node place = minNodeAfterExactByValue(root, value);
            if (place == null || place.key > key) {
                Node insertionHint = null;
                if (place == null) {
                    if (root != null) {
                        place = maxNodeNonNull(root);
                    }
                } else {
                    if (place.value == value) {
                        insertionHint = place;
                    }
                    place = predecessor(place);
                }
                while (place != null && place.key >= key) {
                    Node next = predecessor(place);
                    delete(place);
                    place = next;
                }
                if (insertionHint == null) {
                    insert(key, value);
                } else {
                    insertionHint.key = key;
                }
            }
            return this;
        }

        @Override
        public int getMaximumWithKeyAtMost(double key, int minimumMeaningfulAnswer) {
            Node q = maxNodeBeforeExact(root, key);
            return q == null ? -1 : q.value;
        }

        private Node newNode(double key, int value, Node parent) {
            int idx = offset + size;
            Node rv = allNodes[idx];
            rv.key = key;
            rv.value = value;
            rv.red = true;
            rv.left = null;
            rv.right = null;
            rv.parent = parent;
            ++size;
            return rv;
        }

        private void deleteNode(Node node) {
            --size;
            int lastIndex = offset + size;
            if (node.index != lastIndex) {
                Node other = allNodes[lastIndex];
                allNodes[node.index] = other;
                allNodes[lastIndex] = node;
                other.index = node.index;
                node.index = lastIndex;
            }
        }

        private static boolean isRed(Node node) {
            return node != null && node.red;
        }

        private static boolean isBlack(Node node) {
            return node == null || !node.red;
        }

        private static Node minNodeNonNull(Node node) {
            while (true) {
                Node left = node.left;
                if (left == null) {
                    return node;
                }
                node = left;
            }
        }

        private static Node maxNodeNonNull(Node node) {
            while (true) {
                Node right = node.right;
                if (right == null) {
                    return node;
                }
                node = right;
            }
        }

        private static Node maxNodeBeforeExact(Node node, double key) {
            if (node == null) {
                return null;
            } else {
                Node parent, child = node;
                double childKey;
                do {
                    childKey = child.key;
                    if (key == childKey) {
                        return child;
                    }
                    parent = child;
                    child = key < childKey ? child.left : child.right;
                } while (child != null);
                return key > childKey ? parent : predecessor(parent);
            }
        }

        private static Node minNodeAfterExactByValue(Node node, int value) {
            if (node == null) {
                return null;
            } else {
                Node parent, child = node;
                int childValue;
                do {
                    childValue = child.value;
                    if (value == childValue) {
                        return child;
                    }
                    parent = child;
                    child = value < childValue ? child.left : child.right;
                } while (child != null);
                return value < childValue ? parent : successor(parent);
            }
        }

        // from the usage of insert, we can prove that value does not exist in the tree.
        private void insert(double key, int value) {
            if (root == null) {
                root = newNode(key, value, null);
                root.red = false;
            } else {
                Node parent, child = root;
                boolean lastSmaller;
                do {
                    parent = child;
                    lastSmaller = value < child.value;
                    child = lastSmaller ? child.left : child.right;
                } while (child != null);

                Node z = newNode(key, value, parent);
                if (lastSmaller) {
                    parent.left = z;
                } else {
                    parent.right = z;
                }
                root = fixAfterInsert(root, z);
            }
        }

        private static Node fixAfterInsert(Node root, Node node) {
            Node z = node, zp;
            while (isRed(zp = z.parent)) {
                Node zpp = zp.parent;
                if (zp == zpp.left) {
                    Node t = zpp.right;
                    if (isRed(t)) {
                        zp.red = false;
                        t.red = false;
                        zpp.red = true;
                        z = zpp;
                    } else {
                        if (z == zp.right) {
                            z = zp;
                            root = rotateLeft(root, z);
                            zp = z.parent;
                            zpp = zp.parent;
                        }
                        zp.red = false;
                        zpp.red = true;
                        root = rotateRight(root, zpp);
                    }
                } else { // symmetric cases
                    Node t = zpp.left;
                    if (isRed(t)) {
                        zp.red = false;
                        t.red = false;
                        zpp.red = true;
                        z = zpp;
                    } else {
                        if (z == zp.left) {
                            z = zp;
                            root = rotateRight(root, z);
                            zp = z.parent;
                            zpp = zp.parent;
                        }
                        zp.red = false;
                        zpp.red = true;
                        root = rotateLeft(root, zpp);
                    }
                }
            }
            root.red = false;
            return root;
        }

        private void delete(Node z) {
            if (z != null) {
                Node y = z;
                boolean yIsRed = y.red;
                Node t, tParent;

                if (z.left == null) {
                    t = z.right;
                    root = transplant(root, z, z.right);
                    tParent = z.parent;
                } else if (z.right == null) {
                    t = z.left;
                    root = transplantNonNull(root, z, z.left);
                    tParent = z.parent;
                } else {
                    y = minNodeNonNull(z.right);
                    yIsRed = y.red;
                    t = y.right;

                    if (y.parent == z) {
                        tParent = y;
                    } else {
                        tParent = y.parent;
                        root = transplant(root, y, t);
                        y.right = z.right;
                        y.right.parent = y;
                    }
                    root = transplantNonNull(root, z, y);
                    y.left = z.left;
                    y.left.parent = y;
                    y.red = z.red;
                }

                if (!yIsRed) {
                    root = fixAfterDelete(root, t, tParent);
                }
                deleteNode(z);
            }
        }

        private static Node fixAfterDelete(Node root, Node node, Node parent) {
            Node x = node;
            Node xParent = parent;
            while ((x != root) && isBlack(x)) {
                if (x == xParent.left) {
                    Node w = xParent.right;

                    if (w.red) {
                        w.red = false;
                        xParent.red = true;
                        root = rotateLeft(root, xParent);
                        w = xParent.right;
                    }
                    if (isBlack(w.left) && isBlack(w.right)) {
                        w.red = true;
                        x = xParent;
                    } else {
                        if (isBlack(w.right)) {
                            w.left.red = false;
                            w.red = true;
                            root = rotateRight(root, w);
                            w = xParent.right;
                        }
                        w.red = xParent.red;
                        xParent.red = false;
                        w.right.red = false;
                        root = rotateLeft(root, xParent);
                        x = root;
                    }
                } else { // symmetric cases
                    Node w = xParent.left;

                    if (w.red) {
                        w.red = false;
                        xParent.red = true;
                        root = rotateRight(root, xParent);
                        w = xParent.left;
                    }
                    if (isBlack(w.left) && isBlack(w.right)) {
                        w.red = true;
                        x = xParent;
                    } else {
                        if (isBlack(w.left)) {
                            w.right.red = false;
                            w.red = true;
                            root = rotateLeft(root, w);
                            w = xParent.left;
                        }
                        w.red = xParent.red;
                        xParent.red = false;
                        w.left.red = false;
                        root = rotateRight(root, xParent);
                        x = root;
                    }
                }
                xParent = x.parent;
            }
            if (x != null) {
                x.red = false;
            }
            return root;
        }

        private static Node successor(Node node) {
            if (node.right != null) {
                return minNodeNonNull(node.right);
            } else {
                Node curr = node;
                Node next = curr.parent;
                while (next != null && curr == next.right) {
                    curr = next;
                    next = next.parent;
                }
                return next;
            }
        }

        private static Node predecessor(Node node) {
            if (node.left != null) {
                return maxNodeNonNull(node.left);
            } else {
                Node curr = node;
                Node next = curr.parent;
                while (next != null && curr == next.left) {
                    curr = next;
                    next = next.parent;
                }
                return next;
            }
        }

        private static Node rotateLeft(Node root, Node x) {
            if (x != null) {
                Node newParent = x.right;
                Node newParentLeft = newParent.left;
                x.right = newParentLeft;

                if (newParentLeft != null) {
                    newParentLeft.parent = x;
                }

                root = transplantNonNull(root, x, newParent);

                newParent.left = x;
                x.parent = newParent;
            }
            return root;
        }

        private static Node rotateRight(Node root, Node x) {
            if (x != null) {
                Node newParent = x.left;
                Node newParentRight = newParent.right;
                x.left = newParentRight;

                if (newParentRight != null) {
                    newParentRight.parent = x;
                }

                root = transplantNonNull(root, x, newParent);

                newParent.right = x;
                x.parent = newParent;
            }
            return root;
        }

        private static Node transplant(Node root, Node source, Node target) {
            return target == null ? transplantNull(root, source) : transplantNonNull(root, source, target);
        }

        private static Node transplantNull(Node root, Node source) {
            Node sourceParent = source.parent;
            if (sourceParent == null) {
                return null;
            }
            if (source == sourceParent.left) {
                sourceParent.left = null;
            } else {
                sourceParent.right = null;
            }
            return root;
        }

        private static Node transplantNonNull(Node root, Node source, Node target) {
            Node sourceParent = source.parent;
            target.parent = sourceParent;
            if (sourceParent == null) {
                return target;
            }
            if (source == sourceParent.left) {
                sourceParent.left = target;
            } else {
                sourceParent.right = target;
            }
            return root;
        }
    }
}
