package ru.ifmo.nds.util;

/**
 * This is an implementation of the rank query structure using a tailored version of the Red-Black Tree
 * from the Scala collection library.
 *
 * @author Rui Gonçalves
 * @author Maxim Buzdalov
 */
public final class RedBlackRankQueryStructure extends RankQueryStructure {
    private final Node[] allNodes;

    public RedBlackRankQueryStructure(int maximumPoints) {
        allNodes = new Node[maximumPoints];
        for (int i = 0; i < maximumPoints; ++i) {
            allNodes[i] = new Node();
            allNodes[i].index = i;
        }
    }

    @Override
    public RangeHandle createHandle(int storageStart, int from, int until, int[] indices, int[] values) {
        return new RangeHandleImpl(storageStart);
    }

    private static class Node {
        int key;
        int value;
        int index;
        boolean red;
        Node left, right, parent;
    }

    private final class RangeHandleImpl extends RankQueryStructure.RangeHandle {
        private Node root = null;
        private int size = 0;
        private final int offset;

        private RangeHandleImpl(int storageStart) {
            this.offset = storageStart;
        }

        @Override
        public void put(int key, int value) {
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
        }

        @Override
        public int getMaximumWithKeyAtMost(int key, int minimumMeaningfulAnswer) {
            Node q = maxNodeBeforeExact(root, key);
            return q == null ? -1 : q.value;
        }

        private void setValue(Node node, int value) {
            node.value = value;
        }

        private Node newNode(int key, int value, Node parent) {
            int idx = offset + size;
            Node rv = allNodes[idx];
            rv.key = key;
            setValue(rv, value);
            rv.red = true;
            rv.left = null;
            rv.right = null;
            rv.parent = parent;
            return rv;
        }

        private void deleteNode(Node node) {
            setValue(node, -1);
            int lastIndex = offset + size - 1;
            if (node.index != lastIndex) {
                Node other = allNodes[lastIndex];
                allNodes[node.index] = other;
                allNodes[lastIndex] = node;
                other.index = node.index;
                node.index = lastIndex;
            }
        }

        private boolean isRed(Node node) {
            return node != null && node.red;
        }

        private boolean isBlack(Node node) {
            return node == null || !node.red;
        }

        private Node minNodeNonNull(Node node) {
            while (true) {
                if (node.left == null) {
                    return node;
                }
                node = node.left;
            }
        }

        private Node maxNodeNonNull(Node node) {
            while (true) {
                if (node.right == null) {
                    return node;
                }
                node = node.right;
            }
        }

        private Node maxNodeBeforeExact(Node node, int key) {
            if (node == null) {
                return null;
            } else {
                Node parent = null;
                Node child = node;
                int cmp = 1;
                while (child != null && cmp != 0) {
                    parent = child;
                    cmp = Integer.compare(key, child.key);
                    child = cmp < 0 ? child.left : child.right;
                }
                return cmp >= 0 ? parent : predecessor(parent);
            }
        }

        private Node minNodeAfterExactByValue(Node node, int value) {
            if (node == null) {
                return null;
            } else {
                Node parent = null;
                Node child = node;
                int cmp = -1;
                while (child != null && cmp != 0) {
                    parent = child;
                    cmp = Integer.compare(value, child.value);
                    child = cmp < 0 ? child.left : child.right;
                }
                return cmp <= 0 ? parent : successor(parent);
            }
        }

        private void insert(int key, int value) {
            Node parent = null;
            Node child = root;
            int cmp = 1;
            while (child != null && cmp != 0) {
                parent = child;
                cmp = Integer.compare(key, child.key);
                child = cmp < 0 ? child.left : child.right;
            }

            if (cmp == 0) {
                setValue(parent, value);
            } else {
                Node z = newNode(key, value, parent);

                if (parent == null) {
                    root = z;
                } else if (cmp < 0) {
                    parent.left = z;
                } else {
                    parent.right = z;
                }

                fixAfterInsert(z);
                size += 1;
            }
        }

        private void fixAfterInsert(Node node) {
            Node z = node;
            while (isRed(z.parent)) {
                if (z.parent == z.parent.parent.left) {
                    Node t = z.parent.parent.right;
                    if (isRed(t)) {
                        z.parent.red = false;
                        t.red = false;
                        z.parent.parent.red = true;
                        z = z.parent.parent;
                    } else {
                        if (z == z.parent.right) {
                            z = z.parent;
                            rotateLeft(z);
                        }
                        z.parent.red = false;
                        z.parent.parent.red = true;
                        rotateRight(z.parent.parent);
                    }
                } else { // symmetric cases
                    Node t = z.parent.parent.left;
                    if (isRed(t)) {
                        z.parent.red = false;
                        t.red = false;
                        z.parent.parent.red = true;
                        z = z.parent.parent;
                    } else {
                        if (z == z.parent.left) {
                            z = z.parent;
                            rotateRight(z);
                        }
                        z.parent.red = false;
                        z.parent.parent.red = true;
                        rotateLeft(z.parent.parent);
                    }
                }
            }
            root.red = false;
        }

        private void delete(Node z) {
            if (z != null) {
                Node y = z;
                boolean yIsRed = y.red;
                Node t, tParent;

                if (z.left == null) {
                    t = z.right;
                    transplant(z, z.right);
                    tParent = z.parent;
                } else if (z.right == null) {
                    t = z.left;
                    transplant(z, z.left);
                    tParent = z.parent;
                } else {
                    y = minNodeNonNull(z.right);
                    yIsRed = y.red;
                    t = y.right;

                    if (y.parent == z) {
                        tParent = y;
                    } else {
                        tParent = y.parent;
                        transplant(y, y.right);
                        y.right = z.right;
                        y.right.parent = y;
                    }
                    transplant(z, y);
                    y.left = z.left;
                    y.left.parent = y;
                    y.red = z.red;
                }

                deleteNode(z);
                if (!yIsRed) {
                    fixAfterDelete(t, tParent);
                }
                size -= 1;
            }
        }

        private void fixAfterDelete(Node node, Node parent) {
            Node x = node;
            Node xParent = parent;
            while ((x != root) && isBlack(x)) {
                if (x == xParent.left) {
                    Node w = xParent.right;

                    if (w.red) {
                        w.red = false;
                        xParent.red = true;
                        rotateLeft(xParent);
                        w = xParent.right;
                    }
                    if (isBlack(w.left) && isBlack(w.right)) {
                        w.red = true;
                        x = xParent;
                    } else {
                        if (isBlack(w.right)) {
                            w.left.red = false;
                            w.red = true;
                            rotateRight(w);
                            w = xParent.right;
                        }
                        w.red = xParent.red;
                        xParent.red = false;
                        w.right.red = false;
                        rotateLeft(xParent);
                        x = root;
                    }
                } else { // symmetric cases
                    Node w = xParent.left;

                    if (w.red) {
                        w.red = false;
                        xParent.red = true;
                        rotateRight(xParent);
                        w = xParent.left;
                    }
                    if (isBlack(w.right) && isBlack(w.left)) {
                        w.red = true;
                        x = xParent;
                    } else {
                        if (isBlack(w.left)) {
                            w.right.red = false;
                            w.red = true;
                            rotateLeft(w);
                            w = xParent.left;
                        }
                        w.red = xParent.red;
                        xParent.red = false;
                        w.left.red = false;
                        rotateRight(xParent);
                        x = root;
                    }
                }
                xParent = x.parent;
            }
            if (x != null) {
                x.red = false;
            }
        }

        private Node successor(Node node) {
            if (node.right != null) {
                return minNodeNonNull(node.right);
            } else {
                Node curr = node;
                Node next = curr.parent;
                while ((next != null) && (curr == next.right)) {
                    curr = next;
                    next = next.parent;
                }
                return next;
            }
        }

        private Node predecessor(Node node) {
            if (node.left != null) {
                return maxNodeNonNull(node.left);
            } else {
                Node curr = node;
                Node next = curr.parent;
                while ((next != null) && (curr == next.left)) {
                    curr = next;
                    next = next.parent;
                }
                return next;
            }
        }

        private void rotateLeft(Node x) {
            if (x != null) {
                Node newParent = x.right;
                x.right = newParent.left;

                if (newParent.left != null) {
                    newParent.left.parent = x;
                }
                newParent.parent = x.parent;

                if (x.parent == null) {
                    root = newParent;
                } else if (x == x.parent.left) {
                    x.parent.left = newParent;
                } else {
                    x.parent.right = newParent;
                }

                newParent.left = x;
                x.parent = newParent;
            }
        }

        private void rotateRight(Node x) {
            if (x != null) {
                Node newParent = x.left;
                x.left = newParent.right;

                if (newParent.right != null) {
                    newParent.right.parent = x;
                }
                newParent.parent = x.parent;

                if (x.parent == null) {
                    root = newParent;
                } else if (x == x.parent.right) {
                    x.parent.right = newParent;
                } else {
                    x.parent.left = newParent;
                }

                newParent.right = x;
                x.parent = newParent;
            }
        }

        private void transplant(Node to, Node from) {
            if (to.parent == null) {
                root = from;
            } else if (to == to.parent.left) {
                to.parent.left = from;
            } else {
                to.parent.right = from;
            }

            if (from != null) {
                from.parent = to.parent;
            }
        }
    }
}
