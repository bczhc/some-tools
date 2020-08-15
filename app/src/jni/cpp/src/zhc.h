//
// Created by zhc on 2018/5/7
// Created by root on 19-7-3.
//

#ifndef C99_ZHC_H
#define C99_ZHC_H
#include <iterator>
#ifndef __cplusplus
#include <ctype.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#else
#include <cctype>
#include <cstdio>
#include <cstdlib>
#include <cstring>

#endif
#pragma clang diagnostic push
#pragma ide diagnostic ignored "OCUnusedGlobalDeclarationInspection"
#ifndef ARR_len
#define ARR_len(x) sizeof(x) / sizeof(x)[0]
#endif // ARR_len
#ifdef __cplusplus
extern "C" {
#endif //__cplusplus

char *ToUpperCase(char *Dest, const char *string);

void PrintArr(const char arr[], int len);

int64_t m_pow(int64_t base, int64_t exponent);

int BinToDec(const char *NumStr);

void printArr(const char *a, int length);

char *substring(char *Dest, const char *source, int beginIndex, int endIndex);

void substr(char **Dest, const char *source, int from, int length);

char *substr2(char *Dest, const char *source, int from, int length);

long long getFileSize(FILE *fp);

int getIntegerLen(int x);

int getLongLen(long x);

void Scanf(char **Dest);

void strcpyAndCat_auto(char **Dest, const char *cpy_s, int cpy_s_length,
                       const char *cat_s, int cat_s_length);

void strcat_auto(char **sourceDest, const char *cat_s);

void charToCharPtr(char **Dest, char c);
/**
 *
 * @param string s
 * @param s s
 * @return r
 * @example this("123abc123", "23) = 2 this("12342312452312i23ab", "23") = 4
 * usage:
 * int *p = NULL;
 * int t = this(&p, "a1b1c1", "1"); => p[0] = 1, p[1] = 3, p[2] = 5; t = 3;
 */
uint32_t strInStrCount(int **Dest, const char *string, const char *s);

/**
 * String split
 * @param Dest Dest
 * @param str String
 * @param splitChar as separation
 * @use
 * void ***r = NULL;
 * split(&r, str1, str2);
 * int i = *((int *) (r[0][0]));// element count
 * char **R = ((char **) ((char ***) r)[1]); //char * result
 * for (int j = 0; j < i; ++j) {
        printf("%s\n", R[j]);
    }
 *//*
void split(void ****Dest, char *str, const char *splitChar) {
    *Dest = (void ***) malloc((size_t) (sizeof(char **) * 2));
    ((*Dest)[0]) = (void **) malloc((size_t) (sizeof(int *) * 1));
    (*Dest)[0][0] = (void *) malloc((size_t) (size_t) (sizeof(int)));
    (*Dest)[1] = (void **) malloc((size_t) (sizeof(void *) * 3));
    char *r = NULL;
    uint32_t sS = strlen(str) + 1, splitChar_s = strlen(splitChar) + 1;
    char str_charArr[sS], splitChar_charArr[splitChar_s];
    for (int j = 0; j < sS - 1; ++j) {
        str_charArr[j] = str[j];
    }
    str_charArr[sS - 1] = 0x0;
    for (int j = 0; j < splitChar_s - 1; ++j) {
        splitChar_charArr[j] = splitChar[j];
    }
    splitChar_charArr[splitChar_s - 1] = 0x0;
    uint32_t eC = strInStrCount(str, splitChar) + 1;
    if (str[0] == splitChar[0]) eC--;
    if (str[sS - 2] == splitChar[splitChar_s - 2]) eC--;
    if (eC != 1) {
        goto n;
    }
    *((int *) ((*Dest)[0][0])) = (int) 0;
    strcpy((*Dest)[1][0], "");
    return;
    n:
    *((int *) ((*Dest)[0][0])) = (int) eC;
    (*Dest)[1] = (void **) malloc((size_t) (sizeof(char *) * eC));
    r = strtok(str_charArr, splitChar_charArr);
    int a_i = 0;
    while (r != NULL) {
        int rS = strlen(r) + 1;
        (*Dest)[1][a_i] = ((char *) malloc((size_t) rS));
        strcpy((*Dest)[1][a_i], r);
        r = strtok(NULL, splitChar_charArr);
        ++a_i;
    }
}*/

int Str_Cmp_nMatchCase(const char *a, const char *b);

void m_itoa(char **Dest, int i);

/*void m_lltoa(char **Dest, const int64_t int ll) {

}*/

void m_ltoa(char **Dest, long l);

int split(char ***Dest, const char *SourceString, const char *SplitStr);

int cmpIntArray(const int *a1, int a1Len, const int *a2, int a2Len);

int cmpCharArray(const char *a1, int a1Len, const char *a2, int a2Len);

int charArrToInt(const char *s, size_t size);

int getBiggerNum(int a, int b);

int firstIndexOf(const char *s, int s_len, char c);

char m_itoc(int i);

int m_ctoi(char c);

int Split(char ***dst, const char *s, int s_length, const char *separatorString,
          int separatorString_length);
#ifdef __cplusplus
}

#include <iostream>
using namespace std;

template <typename T> class ArrayList {
private:
  T *arr{};
  int32_t lastIndex{};
  int32_t pSize{};

public:
  ArrayList();

  void add(int32_t index, T a);

  void add(T a);

  bool addAll(int32_t index, ArrayList<T> &list);

  T get(int32_t index);

  int32_t indexOf(T a);

  T remove(int32_t index);

  T set(int32_t index, T a);

  ArrayList<T> subList(int32_t fromIndex, int32_t toIndex);

  int32_t length();

  void release();
};

template <typename T> class Stack {
private:
  int32_t len{};
  int32_t pSize{};
  T *stack{};

public:
  Stack();

  int32_t length();

  int32_t push(T a);

  T pop();

  T peek();

  void release();

  int32_t search(T a);
};

template <typename T> ArrayList<T>::ArrayList() {
  arr = nullptr;
  lastIndex = 0;
  pSize = sizeof(T);
}

template <typename T> void ArrayList<T>::add(int32_t index, T a) {
  ++lastIndex;
  arr = (T *)realloc(arr, (size_t)(pSize * lastIndex));
  for (int32_t i = lastIndex - 1; i > index; --i) {
    arr[i] = arr[i - 1];
  }
  arr[index] = a;
}

template <typename T> void ArrayList<T>::add(T a) {
  ++lastIndex;
  arr = (T *)realloc(arr, (size_t)(pSize * lastIndex));
  arr[lastIndex - 1] = a;
}

template <typename T>
bool ArrayList<T>::addAll(int32_t index, ArrayList<T> &list) {
  int32_t listLength = list.length();
  if (!listLength)
    return false;
  lastIndex += listLength;
  arr = (T *)realloc(arr, (size_t)(pSize * lastIndex));
  int32_t i;
  int32_t t = index + listLength;
  for (i = lastIndex - 1; i >= t; --i) {
    arr[i] = arr[i - listLength];
  }
  for (i = 0; i < listLength; ++i) {
    arr[index + i] = arr[i];
  }
  return true;
}

template <typename T> T ArrayList<T>::get(int32_t index) { return arr[index]; }

template <typename T> int32_t ArrayList<T>::indexOf(T a) {
  for (int32_t i = 0; i < lastIndex; ++i) {
    if (arr[i] == a)
      return i;
  }
  return -1;
}

template <typename T> T ArrayList<T>::remove(int32_t index) {
  T r = arr[index];
  for (int32_t i = index; i < lastIndex; ++i) {
    arr[i] = arr[i - 1];
  }
  --lastIndex;
  arr = (T *)realloc(arr, (size_t)(pSize * lastIndex));
  return r;
}

template <typename T> T ArrayList<T>::set(int32_t index, T a) {
  T r = arr[index];
  arr[index] = a;
  return r;
}

template <typename T>
ArrayList<T> ArrayList<T>::subList(int32_t fromIndex, int32_t toIndex) {
  ArrayList<T> r{};
  for (int32_t i = fromIndex; i < toIndex; ++i) {
    r.add(arr[i]);
  }
  return r;
}

template <typename T> int32_t ArrayList<T>::length() { return this->lastIndex; }

template <typename T> void ArrayList<T>::release() { delete this->arr; }

template <typename T> Stack<T>::Stack() {
  stack = nullptr;
  len = 0;
  pSize = sizeof(T);
}

template <typename T> int32_t Stack<T>::length() { return this->len; }

template <typename T> int32_t Stack<T>::push(T a) {
  ++len;
  stack = (T *)realloc(stack, (size_t)(len * pSize));
  stack[len - 1] = a;
  return len;
}

template <typename T> T Stack<T>::pop() {
  T r = stack[len - 1];
  --len;
  stack = (T *)realloc(stack, (size_t)(len * pSize));
  return r;
}

template <typename T> T Stack<T>::peek() { return stack[len - 1]; }

template <typename T> void Stack<T>::release() { delete this->stack; }

template <typename T> int32_t Stack<T>::search(T a) {
  for (int32_t i = 0; i < len; ++i) {
    if (stack[i] == a)
      return i + 1;
  }
}

class RPN {
public:
};

template <typename T> class Node {
public:
  T data;
  Node *next;
  Node();
};

template <typename T> Node<T>::Node() { this->next = nullptr; }

template <typename T> class LinkedList {
private:
  // the head node, without any data.
  Node<T> *head;
  int32_t len{};
  Node<T> *getNode(int32_t index);

public:
  void put(T a);

  void put(int32_t index, T a);

  void putFirst(T a);

  T removeFirst();

  T removeLast();

  T get(int32_t index);

  void set(int32_t index, T a);

  int32_t length();

  LinkedList();

  ~LinkedList();
};
template <typename T> Node<T> *LinkedList<T>::getNode(int32_t index) {
  if (index == -1)
    return head;
  Node<T> *t = head;
  for (int32_t i = 0; i <= index; ++i) {
    t = t->next;
  }
  return t;
}

template <typename T> LinkedList<T>::LinkedList() {
  this->head = new Node<T>;
  this->len = 0;
}

template <typename T> void LinkedList<T>::put(T a) {
  Node<T> *last = this->getNode(len - 1);
  auto *node = new Node<T>;
  node->data = a;
  last->next = node;
  ++len;
}

template <typename T> void LinkedList<T>::put(int32_t index, T a) {
  Node<T> *t = head;
  Node<T> *needed = nullptr;
  for (int32_t i = 0; i <= index; ++i) {
    if (t->next == nullptr) {
      auto *node = new Node<T>;
      t->next = node;
      ++len;
    }
    t = t->next;
    if (i == index)
      needed = t;
  }
  Node<T> *next = needed->next;
  auto *newNode = new Node<T>;
  newNode->data = a;
  newNode->next = next;
  needed->next = newNode;
  ++len;
}

template <typename T> T LinkedList<T>::get(int32_t index) {
  return this->getNode(index)->data;
}
template <typename T> T LinkedList<T>::removeFirst() {
  Node<T> *root = head->next;
  T removed = root->data;
  head->next = root->next;
  delete root;
  --len;
  return removed;
}

template<typename T>
void LinkedList<T>::set(int32_t index, T a) {
	Node<T> *t = head->next;
	for (int32_t i = 0; i < index; ++i) t = t->next;
	t->data = a;
}

template<typename T>
void LinkedList<T>::putFirst(T a) {
	auto *newNode = new Node<T>;
	newNode->data = a;
	newNode->next = head->next;
	head->next = newNode;
	++len;
}
template <typename T> T LinkedList<T>::removeLast() {
  Node<T> *lastPrev = this->getNode(len - 2);
  T removed = lastPrev->next->data;
  delete lastPrev->next;
  lastPrev->next = nullptr;
  --len;
  return removed;
}
template <typename T> int32_t LinkedList<T>::length() { return this->len; }
template <typename T> LinkedList<T>::~LinkedList() {
  Node<T> *t = head, *next;
  for (int32_t i = 0; i <= len; ++i) {
    next = t->next;
    t = next;
    delete t;
    t = next;
  }
}

class String {
private:
  char *data;
  size_t len;
  void init(const char *s, int32_t length);

public:
  explicit String(const char *s);
  String(const char *s, size_t len);
  ~String();
  char *getData();
  String append(const char *s);
  String append(String &s);
  ArrayList<String *> split(const char *separator);
  ArrayList<String *> split(String &separator);
  string toCppString();
  String &operator=(const char *s);
};

#endif //__cplusplus

#endif // C99_ZHC_H
