package rest.felix.back.todo.service;

import java.security.SecureRandom;
import java.util.Objects;

public class OrderGenerator {

  private static final String CHARSET = "0123456789abcdefghijklmnopqrstuvwxyz";
  private static final int MAX_LENGTH = 256;
  private static final SecureRandom random = new SecureRandom();

  /**
   * lexicographical order를 유지하는 문자열 생성 함수
   *
   * @param left 왼쪽 경계 문자열 (선택적)
   * @param right 오른쪽 경계 문자열 (선택적)
   * @return 사이에 삽입될 문자열
   */
  public static String generate(String left, String right) {
    if (left == null && right == null) {
      return "i";
    }
    if (left != null && right == null) {
      return generateAfter(left);
    }
    if (left == null && right != null) {
      return generateBefore(right);
    }
    return generateBetween(Objects.requireNonNull(left), Objects.requireNonNull(right));
  }

  /** 중간값에 해당하는 문자 생성 (약간의 임의성 포함) */
  private static char generateMiddleChar() {
    int midIndex = CHARSET.length() / 2;
    int randomOffset = random.nextInt(5) - 2; // -2 ~ 2
    int targetIndex =
        Math.max(
            1, // '0'을 반환하지 않도록 최소값을 1로 설정
            Math.min(CHARSET.length() - 1, midIndex + randomOffset));
    return CHARSET.charAt(targetIndex);
  }

  /** 주어진 문자열 뒤에 올 문자열 생성 */
  private static String generateAfter(String str) {
    if (str.length() >= MAX_LENGTH) {
      throw new IllegalStateException("String too long");
    }
    return str + generateMiddleChar();
  }

  /** 주어진 문자열 앞에 올 문자열 생성 */
  private static String generateBefore(String str) {
    if (str.length() >= MAX_LENGTH) {
      throw new IllegalStateException("String too long");
    }

    char minChar = CHARSET.charAt(0);
    StringBuilder prefix = new StringBuilder();
    int i = 0;

    while (i < str.length() && str.charAt(i) == minChar) {
      prefix.append(minChar);
      i++;
    }

    if (i == str.length()) {
      throw new IllegalStateException("Cannot generate a key before an all-minimum-character key.");
    }

    char charAtIndex = str.charAt(i);
    int charIndex = CHARSET.indexOf(charAtIndex);

    int midIndex = charIndex / 2;

    if (midIndex == 0) {
      return prefix.toString() + minChar + generateMiddleChar();
    } else {
      return prefix.toString() + CHARSET.charAt(midIndex);
    }
  }

  /** 두 문자열 사이의 중간값 생성 */
  private static String generateBetween(String left, String right) {
    if (left.compareTo(right) >= 0) {
      throw new IllegalArgumentException("Left must be less than right");
    }

    StringBuilder result = new StringBuilder();
    int i = 0;

    while (true) {
      char leftChar = i < left.length() ? left.charAt(i) : CHARSET.charAt(0);
      char rightChar = i < right.length() ? right.charAt(i) : CHARSET.charAt(CHARSET.length() - 1);

      int leftIndex = CHARSET.indexOf(leftChar);
      int rightIndex = CHARSET.indexOf(rightChar);

      if (leftIndex == rightIndex) {
        result.append(leftChar);
        i++;
        continue;
      }

      if (rightIndex - leftIndex > 1) {
        int midIndex = (leftIndex + rightIndex) / 2;
        return result.toString() + CHARSET.charAt(midIndex);
      } else {
        result.append(leftChar);
        left = left.substring(i + 1);
        right = ""; // 오른쪽 경계는 무시하고 왼쪽 기준 다음으로 생성
        i = 0; // 인덱스 초기화
      }
    }
  }
}
