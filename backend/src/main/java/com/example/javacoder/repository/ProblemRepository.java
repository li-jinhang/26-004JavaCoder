package com.example.javacoder.repository;

import com.example.javacoder.model.ExampleCase;
import com.example.javacoder.model.Problem;
import com.example.javacoder.model.TestCase;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ProblemRepository {

    private final Map<Long, Problem> problems = Map.of(
            1L, new Problem(
                    1,
                    "两数之和",
                    "简单",
                    List.of("数组", "哈希表"),
                    "给定一个整数数组 nums 和一个整数 target，请返回两个下标，使这两个下标对应的数之和等于 target。每组输入保证恰好存在一个答案。",
                    "第一行包含 n 和 target。第二行包含 n 个整数。",
                    "按升序输出两个下标，中间用一个空格分隔。",
                    "2 <= n <= 10000, -1000000000 <= nums[i], target <= 1000000000",
                    """
                            import java.util.*;

                            public class Main {
                                public static void main(String[] args) {
                                    Scanner scanner = new Scanner(System.in);
                                    int n = scanner.nextInt();
                                    int target = scanner.nextInt();
                                    int[] nums = new int[n];
                                    for (int i = 0; i < n; i++) {
                                        nums[i] = scanner.nextInt();
                                    }

                                    // TODO: 输出两个下标，例如 System.out.println(i + " " + j);
                                }
                            }
                            """,
                    List.of(
                            new ExampleCase("4 9\n2 7 11 15\n", "0 1", "nums[0] + nums[1] = 9"),
                            new ExampleCase("3 6\n3 2 4\n", "1 2", "nums[1] + nums[2] = 6")
                    ),
                    List.of(
                            new TestCase("4 9\n2 7 11 15\n", "0 1", false),
                            new TestCase("3 6\n3 2 4\n", "1 2", false),
                            new TestCase("2 6\n3 3\n", "0 1", true),
                            new TestCase("5 10\n1 8 3 7 5\n", "2 3", true)
                    )
            ),
            2L, new Problem(
                    2,
                    "有效的括号",
                    "简单",
                    List.of("栈", "字符串"),
                    "给定一个只包含括号字符的字符串 s，判断它是否有效。左括号必须由相同类型的右括号闭合，并且闭合顺序必须正确。",
                    "一行字符串 s。",
                    "如果字符串有效，输出 true；否则输出 false。",
                    "1 <= s.length <= 10000",
                    """
                            import java.util.*;

                            public class Main {
                                public static void main(String[] args) {
                                    Scanner scanner = new Scanner(System.in);
                                    String s = scanner.nextLine();

                                    // TODO: 判断括号字符串是否有效
                                    System.out.println(false);
                                }
                            }
                            """,
                    List.of(
                            new ExampleCase("()[]{}\n", "true", "每个左括号都由相同类型的右括号闭合。"),
                            new ExampleCase("(]\n", "false", "括号类型不匹配。")
                    ),
                    List.of(
                            new TestCase("()[]{}\n", "true", false),
                            new TestCase("(]\n", "false", false),
                            new TestCase("([{}])\n", "true", true),
                            new TestCase("(((()))\n", "false", true)
                    )
            ),
            3L, new Problem(
                    3,
                    "最长递增子序列",
                    "中等",
                    List.of("动态规划", "二分查找"),
                    "给定一个整数数组 nums，请返回其中最长严格递增子序列的长度。",
                    "第一行包含 n。第二行包含 n 个整数。",
                    "输出一个整数，表示最长递增子序列的长度。",
                    "1 <= n <= 2500, -10000 <= nums[i] <= 10000",
                    """
                            import java.util.*;

                            public class Main {
                                public static void main(String[] args) {
                                    Scanner scanner = new Scanner(System.in);
                                    int n = scanner.nextInt();
                                    int[] nums = new int[n];
                                    for (int i = 0; i < n; i++) {
                                        nums[i] = scanner.nextInt();
                                    }

                                    // TODO: 输出最长递增子序列长度
                                    System.out.println(0);
                                }
                            }
                            """,
                    List.of(
                            new ExampleCase("8\n10 9 2 5 3 7 101 18\n", "4", "其中一个最长递增子序列是 2, 3, 7, 101。"),
                            new ExampleCase("6\n0 1 0 3 2 3\n", "4", "其中一个最长递增子序列是 0, 1, 2, 3。")
                    ),
                    List.of(
                            new TestCase("8\n10 9 2 5 3 7 101 18\n", "4", false),
                            new TestCase("6\n0 1 0 3 2 3\n", "4", false),
                            new TestCase("7\n7 7 7 7 7 7 7\n", "1", true),
                            new TestCase("9\n4 10 4 3 8 9 11 2 12\n", "5", true)
                    )
            )
    );

    public Collection<Problem> findAll() {
        return problems.values();
    }

    public Optional<Problem> findById(long id) {
        return Optional.ofNullable(problems.get(id));
    }
}
