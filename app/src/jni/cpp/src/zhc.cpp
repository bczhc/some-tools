//
// Created by root on 19-7-3.
//

#include "zhc.h"

char *ToUpperCase(char *Dest, const char *string) {
  char *p = Dest;
  int len = strlen(string);
  char r[len + 1];
  int i = 0;
  while (1) {
    r[i] = (char)toupper((int)string[i]);
    if (string[i] == '\0')
      break;
    i++;
  }
  strcpy(p, r);
  return Dest;
}

void PrintArr(const char arr[], int len) {
  int l_ = len - 1;
  printf("[");
  for (int i = 0; i < l_; ++i) {
    printf("%i%c", (int)arr[i], 44);
  }
  printf("%i]___%u", (int)arr[l_ - 1], (l_ + 1));
}

int64_t m_pow(const int64_t base, const int64_t exponent) {
  int64_t r = 1LL;
  for (int i = 0; i < exponent; ++i) {
    r *= base;
  }
  return r;
}

int BinToDec(const char *NumStr) {
  int r = 0;
  int j = 0;
  for (int i = strlen(NumStr) - 1; i >= 0; --i) {
    r += (NumStr[i] == '0' ? 0 : 1) * m_pow(2, j);
    j++;
  }
  return r;
}

void printArr(const char *a, const int length) {
  int l = length;
  printf("[");
  for (int i = 0; i < l; ++i) {
    printf("%i", (int)a[i]);
    if (i != l - 1) {
      printf(",");
    }
  }
  printf("]\n");
}

char *substring(char *Dest, const char *source, const int beginIndex,
                const int endIndex) {
  char *r = Dest;
  strncpy(r, source + beginIndex, (size_t)(endIndex - beginIndex));
  return Dest;
}

void substr(char **Dest, const char *source, const int from, int length) {
  *Dest = (char *)malloc((size_t)length + 1);
  strncpy(*Dest, source + from, (size_t)length);
}

char *substr2(char *Dest, const char *source, const int from, int length) {
  char *r = Dest;
  strncpy(r, source + from, (size_t)length);
  return Dest;
}

long long getFileSize(FILE *fp) {
  long long sz;
  fseek(fp, 0L, SEEK_END);
  sz = (long long)ftell(fp);
  if (sz == -1) {
    //        sz = _ftelli64(fp);
    printf("Get file size error.\n");
  }
  fseek(fp, 0L, SEEK_SET);
  return sz;
}

int getIntegerLen(const int x) {
  int n = x;
  int r = 0;
  while (1) {
    int b = n / 10;
    r++;
    n = b;
    if (!b)
      break;
  }
  return r;
}

int getLongLen(const long x) {
  long n = x;
  int r = 0;
  while (1) {
    long b = n / 10;
    r++;
    n = b;
    if (!b)
      break;
  }
  return r;
}

void Scanf(char **Dest) {
  char c;
  int i = 1;
  while (1) {
    scanf("%c", &c);
    *Dest = (char *)realloc(*Dest, (size_t)i);
    if (c == 0x0A) {
      (*Dest)[i - 1] = 0x0;
      break;
    }
    (*Dest)[i - 1] = c;
    ++i;
  }
}

void strcpyAndCat_auto(char **Dest, const char *cpy_s, int cpy_s_length,
                       const char *cat_s, int cat_s_length) {
  *Dest = NULL;
  if (cpy_s_length == -1)
    cpy_s_length = strlen(cpy_s);
  if (cat_s_length == -1)
    cat_s_length = strlen(cat_s);
  int cpy_s_len = cpy_s_length;
  int cat_s_len = cat_s_length;
  size_t size = cpy_s_len + cat_s_len + 1;
  *Dest = (char *)malloc(size);
  strncpy(*Dest, cpy_s, cpy_s_length);
  strncat(*Dest, cat_s, cat_s_length);
  (*Dest)[size - 1] = '\0';
}

void strcat_auto(char **sourceDest, const char *cat_s) {
  if (*sourceDest == NULL) {
    *sourceDest = (char *)malloc(1);
    (*sourceDest)[0] = 0;
  }
  int sourceLen = strlen(*sourceDest);
  char cloneSource[sourceLen + 1];
  strcpy(cloneSource, *sourceDest);
  size_t size = sourceLen + strlen(cat_s) + 1;
  *sourceDest = (char *)malloc(size);
  strcpy(*sourceDest, cloneSource);
  strcat(*sourceDest, cat_s);
  (*sourceDest)[size - 1] = '\0';
}

void charToCharPtr(char **Dest, const char c) {
  *Dest = NULL;
  *Dest = (char *)malloc((size_t)2);
  (*Dest)[0] = c;
}

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
uint32_t strInStrCount(int **Dest, const char *string, const char *s) {
  uint32_t c = 0;
  uint32_t stringL = strlen(string), sL = strlen(s);
  uint32_t forI = stringL - sL + 1;
  *Dest = NULL;
  if (stringL < sL) {
    //        free((void *) forI);
    return 0;
  } else {
    for (int i = 0; i < forI; ++i) {
      int b = 1;
      for (int j = 0; j < sL; ++j) {
        b &= (string[i + j] == s[j]);
      }
      if (b) {
        *Dest = (int *)realloc(*Dest, (size_t)(4 * (++c)));
        (*Dest)[c - 1] = i;
      }
    }
  }
  return c;
}

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

int Str_Cmp_nMatchCase(const char *a, const char *b) {
  char t1[strlen(a) + 1];
  char t2[strlen(a) + 1];
  ToUpperCase(t1, a);
  ToUpperCase(t2, b);
  return strcmp(t1, t2) ? 0 : 1;
}

void m_itoa(char **Dest, const int i) {
  int I_L = getIntegerLen(i);
  *Dest = (char *)malloc((size_t)(I_L + 1));
  int d_i = 0;
  for (int j = I_L - 1; j >= 0; --j) {
    (*Dest)[d_i] = (int)(((long)i) / ((long)m_pow(10LL, j)) % 10) + 48;
    ++d_i;
  }
  (*Dest)[d_i] = 0;
}

/*void m_lltoa(char **Dest, const int64_t int ll) {

}*/

void m_ltoa(char **Dest, const long i) {
  int I_L = getLongLen(i);
  *Dest = (char *)malloc((size_t)(I_L + 1));
  long d_i = 0;
  for (long j = I_L - 1; j >= 0; --j) {
    (*Dest)[d_i] =
        (int)(((long long)i) / ((long long)m_pow(10LL, j)) % 10) + 48;
    ++d_i;
  }
  (*Dest)[d_i] = 0;
}

int split(char ***Dest, const char *SourceString, const char *SplitStr) {
  int *pos = NULL;
  int posL = strInStrCount(&pos, SourceString, SplitStr);
  uint32_t srcLen = strlen(SourceString), splitStrLen = strlen(SplitStr),
           toP = srcLen - splitStrLen;
  int lastIndex = 0;
  *Dest = (char **)malloc((size_t)(sizeof(char *) * (posL + 1)));
  for (int i = 0; i < posL; ++i) {
    int sL = 0;
    for (int j = lastIndex; j < pos[i]; ++j) {
      sL = pos[i] - lastIndex + 2;
      (*Dest)[i] = (char *)malloc((size_t)(sL));
      (*Dest)[i][j - lastIndex] = SourceString[j];
    }
    (*Dest)[i][pos[i]] = 0;
    lastIndex = pos[i];
  }
  return posL;
}

int cmpIntArray(const int *a1, const int a1Len, const int *a2,
                const int a2Len) {
  if (a1Len != a2Len)
    return 0;
  else {
    for (int i = 0; i < a1Len; ++i) {
      if (a1[i] != a2[i])
        return 0;
    }
  }
  return 1;
}

int cmpCharArray(const char *a1, const int a1Len, const char *a2,
                 const int a2Len) {
  if (a1Len != a2Len)
    return 0;
  else {
    for (int i = 0; i < a1Len; ++i) {
      if (a1[i] != a2[i])
        return 0;
    }
  }
  return 1;
}

int charArrToInt(const char *s, size_t size) {
  int r = 0;
  for (int i = 0; i < size; ++i) {
    r += ((uint32_t)s[i] - 48) * m_pow(10LL, size - i - 1);
  }
  return r;
}

int getBiggerNum(const int a, const int b) { return a > b ? a : b; }

int firstIndexOf(const char *s, const int s_len, const char c) {
  for (int i = 0; i < s_len; ++i) {
    if (s[i] == c)
      return i;
  }
  return -1;
}

char m_itoc(const int i) { return (char)(i + 48); }

int m_ctoi(const char c) { return (int)c - 48; }

int charsCmp(const char *longerS, const int longerS_length,
             const char *shorterS, const int shorterS_length,
             const int longerS_startIndex) {
  int r = 1;
  for (int i = longerS_startIndex; i < longerS_startIndex + shorterS_length;
       ++i) {
    if (longerS_startIndex == longerS_length)
      return 0;
    if (longerS[i] != shorterS[i - longerS_startIndex])
      return 0;
  }
  return 1;
}

char **charcat(char **dst, const int addChar) {
  if (*dst == NULL) {
    *dst = (char *)malloc((size_t)(2));
    (*dst)[0] = addChar;
    (*dst)[1] = '\0';
  } else {
    int len = strlen(*dst);
    *dst = (char *)realloc(*dst, len + 2);
    (*dst)[len] = addChar;
    (*dst)[len + 1] = '\0';
  }
  return dst;
}

int Split(char ***dst, const char *s, int s_length, const char *separatorString,
          int separatorString_length) {
  *dst = (char **)malloc((size_t)(sizeof(char *) * 1)),
  (*dst)[0] = (char *)malloc((size_t)1), (*dst)[0][0] = '\0';
  if (s_length == -1)
    s_length = strlen(s);
  if (separatorString_length == -1)
    separatorString_length = strlen(separatorString);
  int pLen = sizeof(char *);
  int dstI = 0;
  int t = 0;
  for (int i = 0; i < s_length; ++i) {
    if (charsCmp(s, s_length, separatorString, separatorString_length, i) ==
        1) {
      ++dstI;
      *dst = (char **)realloc(*dst, (size_t)((dstI + 1) * pLen)),
      (*dst)[dstI] = (char *)malloc((size_t)1), (*dst)[dstI][0] = '\0';
      t = separatorString_length;
    }
    if (t-- > 0)
      continue;
    charcat(&((*dst)[dstI]), s[i]);
  }
  return dstI + 1;
}

void String::init(const char *s, int32_t length) {
  len = length;
  data = (char *)malloc((size_t)(length + 1));
  data[length] = '\0';
  strcpy(data, s);
}


String::String(const char *s) : String(s, strlen(s)) {}

String::String(const char *s, size_t length) {
	this->init(s, length);
}

String::~String() {
  delete data;
  len = 0;
}

char *String::getData() { return data; }

String String::append(const char *s) {
  size_t length = strlen(s);
  size_t newSize = this->len + length + 1;
  char buf[newSize];
  memset(buf, newSize, '\0');
  strcpy(buf, data), strcat(buf, s);
  String theStr(buf, newSize - 1);
  return theStr;
}

String String::append(String &s) {
  char *sData = s.getData();
  return this->append(sData);
}

ArrayList<String *> String::split(const char *separator) {
  char **r = nullptr;
  char *sData = this->getData();
  int32_t arrLength = Split(&r, sData, -1, separator, -1);
  ArrayList<String *> list;
  for (int32_t i = 0; i < arrLength; ++i) {
    String *s = new String(r[i]);
    list.add(s);
    delete (r[i]);
  }
  delete r;
  return list;
}

ArrayList<String *> String::split(String &separator) {
  char *separatorData = separator.getData();
  return this->split(separatorData);
}


string String::toCppString() {
	string r(this->getData());
	return r;
}

String &String::operator=(const char *s) {
	this->init(s, strlen(s));
	return *this;
}
