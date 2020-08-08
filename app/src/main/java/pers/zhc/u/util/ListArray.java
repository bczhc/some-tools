package pers.zhc.u.util;

import java.util.ArrayList;
import java.util.List;

public class ListArray<T> {
    private final List<T> list;

    public ListArray() {
        list = new ArrayList<>();
    }

    public static void main(String[] args) {
        ListArray<Integer> listArray = new ListArray<>();
        listArray.set(0, 1);
        listArray.set(3, 2);
        listArray.set(6, 3);
        listArray.set(3, 4);
        int size = listArray.size();
        System.out.println("size = " + size);
        for (int i = 0; i < size; i++) {
            System.out.println("listArray.get(" + i + ") = " + listArray.get(i));
        }
    }

    public List<T> getList() {
        return list;
    }

    public ListArray<T> set(int index, T value) {
        int size = list.size();
        if (index >= size) {
            int t = index - size + 1;
            for (int i = 0; i < t; i++) {
                list.add(null);
            }
        }
        list.set(index, value);
        return this;
    }

    public int size() {
        return this.list.size();
    }

    public T get(int index) throws IndexOutOfBoundsException {
        return list.get(index);
    }
}
