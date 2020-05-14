package com.xliic.oas.bundler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;

public class Inventory implements Iterable<Inventory.Entry> {

    ArrayList<Inventory.Entry> inventory = new ArrayList<Inventory.Entry>();

    public void add(Entry entry) {
        Entry existingEntryToRemove = null;
        for (Entry existing : inventory) {
            if (existing.parent == entry.parent && existing.key.equals(entry.key)) {
                if (existing.depth > entry.depth || existing.indirections > entry.indirections) {
                    existingEntryToRemove = entry;
                    break;
                } else {
                    return;
                }
            }
        }

        if (existingEntryToRemove != null) {
            inventory.remove(existingEntryToRemove);
        }

        inventory.add(entry);
    }

    @Override
    public Iterator<Inventory.Entry> iterator() {
        return inventory.iterator();
    }

    public int size() {
        return inventory.size();
    }

    public void sort() {
        inventory.sort(new EntryComparator());
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings("SE_COMPARATOR_SHOULD_BE_SERIALIZABLE")
    static class EntryComparator implements Comparator<Entry> {

        @Override
        public int compare(Entry a, Entry b) {
            if (!a.file.equals(b.file)) {
                return a.file.compareTo(b.file);
            } else if (!a.pointer.equals(b.pointer)) {
                return a.pointer.compareTo(b.pointer);
            } else if (a.indirections != b.indirections) {
                // TODO test that lower indirections come first
                return Integer.compare(a.indirections, b.indirections);
            } else if (a.depth != b.depth) {
                // TODO test that lower depth come first
                return Integer.compare(a.depth, b.depth);
            } else if (a.extended != b.extended) {
                // If the $ref extends the resolved value, then sort it lower than other $refs
                // that don't extend the value
                return a.extended ? +1 : -1;
            }

            return 0;
        }

    }

    public static class Entry {
        Document.Part part;
        JsonNode ref;
        JsonNode parent;
        JsonNode value;
        String key;
        JsonPath pathFromRoot;
        JsonPath path;
        String pointer;
        String file;
        int depth;
        int indirections;
        boolean extended;

        public Entry(JsonNode parent, String key, JsonNode ref, JsonNode value, JsonPath pathFromRoot, String file,
                String pointer, JsonPath path, int indirections, Document.Part part) {
            this.ref = ref;
            this.parent = parent;
            this.value = value;
            this.key = key;
            this.pathFromRoot = pathFromRoot;
            this.part = part;
            this.depth = pathFromRoot.size();
            this.indirections = indirections;
            this.pointer = pointer;
            this.path = path;
            this.file = file;
            this.extended = Resolver.isExtendedRef(ref);
        }
    }

}