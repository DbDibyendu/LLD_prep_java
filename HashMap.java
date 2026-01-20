import java.util.*;

/*
 =====================================================
 FUNCTIONAL REQUIREMENTS
 =====================================================
 - put(key, value)
 - get(key)
 - remove(key)

 NON-FUNCTIONAL REQUIREMENTS
 =====================================================
 - O(1) average time complexity
 - Handle hash collisions
 - Generic (supports any key/value type)
 - Clean, maintainable design

 DESIGN DECISIONS
 =====================================================
 - Array of buckets
 - Each bucket uses a Linked List (Separate Chaining)
 - index = hash(key) % capacity

 DESIGN PRINCIPLES USED
 =====================================================
 - SRP: Each class has a single responsibility
 - Encapsulation: Internal structure hidden
 - Generics: Reusability
 - Fail-safe null handling
 =====================================================
*/


// =========================
// Node (Linked List Node)
// =========================
// - SRP: Represents a single entry
// - Used internally by HashMap
class Node<K, V> {
    K key;
    V value;
    Node<K, V> next;

    Node(K key, V value) {
        this.key = key;
        this.value = value;
        this.next = null;
    }
}


// =========================
// MyHashMap (Core Class)
// =========================
// - Encapsulates hashing + collision logic
// - Separate chaining used for collisions
class MyHashMap<K, V> {

    private static final int DEFAULT_CAPACITY = 16;

    private Node<K, V>[] buckets;
    private int size;
    private int capacity;

    // Constructor
    public MyHashMap() {
        this.capacity = DEFAULT_CAPACITY;
        this.buckets = new Node[capacity];
        this.size = 0;
    }

    /*
     Hash function
     - Uses key.hashCode()
     - Converts to valid array index
     - Null key mapped to bucket 0 (same as Java HashMap)
    hashCode() is an Object method that returns a stable integer
    during a program’s execution as long as the object’s equality-defining
    fields don’t change. HashMap relies on this guarantee to locate keys correctly.
    */
    private int getBucketIndex(K key) {
        if (key == null) return 0;
        return Math.abs(key.hashCode()) % capacity;
    }

    /*
     put(key, value)
     - If key exists → update value
     - Else → insert new node
     - Collision handled via linked list
    */
    public void put(K key, V value) {

        int index = getBucketIndex(key);
        Node<K, V> head = buckets[index];

        // Check if key already exists → update
        Node<K, V> curr = head;
        while (curr != null) {
            if (Objects.equals(curr.key, key)) {
                curr.value = value;
                return;
            }
            curr = curr.next;
        }

        // Insert at head (O(1))
        Node<K, V> newNode = new Node<>(key, value);
        newNode.next = head;
        buckets[index] = newNode;
        size++;
    }

    /*
     get(key)
     - Traverse bucket linked list
     - Return value if found
    */
    public V get(K key) {
        int index = getBucketIndex(key);
        Node<K, V> head = buckets[index];

        while (head != null) {
            if (Objects.equals(head.key, key)) {
                return head.value;
            }
            head = head.next;
        }
        return null;
    }

    /*
     remove(key)
     - Adjust linked list pointers
     - Handle head removal
    */
    public void remove(K key) {
        int index = getBucketIndex(key);
        Node<K, V> head = buckets[index];

        Node<K, V> prev = null;
        Node<K, V> curr = head;

        while (curr != null) {
            if (Objects.equals(curr.key, key)) {
                if (prev == null) {
                    buckets[index] = curr.next; // remove head, as update buckets[indext] to the next node
                } else {
                    prev.next = curr.next;
                }
                size--;
                return;
            }
            prev = curr;
            curr = curr.next;
        }
    }

    public int size() {
        return size;
    }
}


// =========================
// Main (Driver / Demo)
// =========================
public class Main {

    public static void main(String[] args) {

        MyHashMap<String, Integer> map = new MyHashMap<>();

        map.put("apple", 10);
        map.put("banana", 20);
        map.put("orange", 30);

        // Update existing key
        map.put("apple", 100);

        System.out.println(map.get("apple"));   // 100
        System.out.println(map.get("banana"));  // 20
        System.out.println(map.get("grape"));   // null

        map.remove("banana");

        System.out.println(map.get("banana"));  // null
        System.out.println("Size: " + map.size());
    }
}
