package com.geekzhang.algorithms.niuke;

import java.util.HashMap;
import java.util.Map;

/**
 * @program: MAP+链表实现Lru
 * @description: https://www.nowcoder.com/practice/5dfded165916435d9defb053c63f1e84?tpId=117&tqId=37804&rp=1&ru=/exam/oj&qru=/exam/oj&sourceUrl=%2Fexam%2Foj%3Ftab%3D%25E7%25AE%2597%25E6%25B3%2595%25E7%25AF%2587%26topicId%3D117&difficulty=undefined&judgeStatus=undefined&tags=&title=
 * @author: zwm
 * @create: 2022-04-12 15:24
 **/
public class LruCache {
    private Node head;
    private Node tail;
    private int capacity;
    private int used;
    Map<Integer ,Node> map;


    public LruCache(int capacity){
        this.capacity = capacity;
        this.map = new HashMap<>();

    }
    public int get(int key){
        if(!map.containsKey(key)){
            return -1;
        }
        return  map.get(key).val;
    }

    public void set(int value,int key){
        if(map.containsKey(key)){
            map.get(key).val = value;
            makeForward(key);
            return;
        }
        if(capacity == used){
            tail = tail.pre;
            tail.next = null;
            used--;
        }
        if(head == null){
            Node node=new Node(key,value,null,null);
            head = node;
            tail = node;
        }else{
            Node node=new Node(key,value,null,head);
            head.pre = node;
            head = node;
        }
        map.put(key,head);
        used++;
    }

     class Node{
        int key;
         Node pre;
         Node next;
        int val;
        Node(int key,int value,Node pre,Node next){
            this.key = key;
            this.val = value;
            this.pre = pre;
            this.next = next;
        }
    }



    private void makeForward(int key){
        Node node = map.get(key);
        if(node != head){
            if(node == tail){
                tail = tail.pre;
                tail.next = null;
            }else{
                node.next.pre = node.pre;
                node.pre.next = node.next;
            }
            node.pre = null;
            head.pre = node;
            node.next = head;
            head = node;
        }
    }
}
