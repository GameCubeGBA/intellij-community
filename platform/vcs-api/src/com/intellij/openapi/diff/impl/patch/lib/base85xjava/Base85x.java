package com.intellij.openapi.diff.impl.patch.lib.base85xjava;

/**
 * The main Base85x-java program
 *
 * @author Simon Warta, Kullo, Nadya Zabrodina
 * @version 0.2
 */
public class Base85x {

  private static final char[] ALPHABET_85 = {
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
    'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
    'U', 'V', 'W', 'X', 'Y', 'Z',
    'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
    'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
    'u', 'v', 'w', 'x', 'y', 'z',
    '!', '#', '$', '%', '&', '(', ')', '*', '+', '-',
    ';', '<', '=', '>', '?', '@', '^', '_', '`', '{',
    '|', '}', '~'
  };

  private static final int[] INDEX_OF = initIndexOfChar();

  private static int[] initIndexOfChar() {
    int[] result = new int[256];
    for (int i = 0; i < ALPHABET_85.length; i++) {
      result[ALPHABET_85[i]] = i;
    }
    return result;
  }

  public static char encodeChar(int i) {
    return ALPHABET_85[i];
  }

  public static int decodeChar(char c) {
    return INDEX_OF[(int)c];
  }

  public static byte[] decode(String data) {
    return decode(data.toCharArray());
  }

  public static char[] encode(byte[] data) {
    int length = data.length;
    char[] out = new char[(length / 4) * 5 + ((length % 4 != 0) ? length % 4 + 1 : 0)];
    int k = 0;
    // 64 bit integer
    long b;
    int c1, c2, c3, c4, c5;
    int rest;
    int i;

    for (i = 0; i + 4 <= length; i += 4) {
      b = 0L;
      b |= (int)data[i] & 0xFF;
      b <<= 8;
      b |= (int)data[i + 1] & 0xFF;
      b <<= 8;
      b |= (int)data[i + 2] & 0xFF;
      b <<= 8;
      b |= (int)data[i + 3] & 0xFF;

      c5 = (int)(b % 85);
      b /= 85;
      c4 = (int)(b % 85);
      b /= 85;
      c3 = (int)(b % 85);
      b /= 85;
      c2 = (int)(b % 85);
      b /= 85;
      c1 = (int)(b % 85);

      out[k] = encodeChar(c1);
      k++;
      out[k] = encodeChar(c2);
      k++;
      out[k] = encodeChar(c3);
      k++;
      out[k] = encodeChar(c4);
      k++;
      out[k] = encodeChar(c5);
      k++;
    }
    if ((rest = length % 4) != 0) {
      int j;
      byte[] block = {'~', '~', '~', '~'};
      for (j = 0; j < rest; j++) {
        block[j] = data[i + j];
      }
      char[] out_rest = encode(block);
      for (j = 0; j < rest + 1; j++) {
        out[k] = out_rest[j];
        k++;
      }
    }
    return out;
  }

  public static byte[] decode(char[] data) {
    int length = data.length;
    byte[] out = new byte[(length / 5) * 4 + ((length % 5 != 0) ? length % 5 - 1 : 0)];
    int k = 0;
    int rest;
    int i;
    int b1, b2, b3, b4, b5;
    int b;

    for (i = 0; i + 5 <= length; i += 5) {
      b1 = decodeChar(data[i]);
      b2 = decodeChar(data[i + 1]);
      b3 = decodeChar(data[i + 2]);
      b4 = decodeChar(data[i + 3]);
      b5 = decodeChar(data[i + 4]);

      // overflow into negative numbers
      // is normal and does not do any damage because
      // of the cut operations below
      b = b1 * 52200625 + b2 * 614125 + b3 * 7225 + b4 * 85 + b5;

      out[k] = (byte)((b >>> 24) & 0xFF);
      k++;
      out[k] = (byte)((b >>> 16) & 0xFF);
      k++;
      out[k] = (byte)((b >>> 8) & 0xFF);
      k++;
      out[k] = (byte)(b & 0xFF);
      k++;
    }

    if ((rest = length % 5) != 0) {
      int j;
      char[] block = {'~', '~', '~', '~', '~'};
      for (j = 0; j < rest; j++) {
        block[j] = data[i + j];
      }
      byte[] out_rest = decode(block);
      for (j = 0; j < rest - 1; j++) {
        out[k] = out_rest[j];
        k++;
      }
    }

    return out;
  }
}
