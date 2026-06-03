package com.example.javacoder.repository;

import com.example.javacoder.model.ExampleCase;
import com.example.javacoder.model.Problem;
import com.example.javacoder.model.TestCase;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;

@Repository
public class ProblemRepository {

    private final Map<Long, Problem> problems = List.of(
            new Problem(
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
                            ex("4 9\n2 7 11 15\n", "0 1", "nums[0] + nums[1] = 9"),
                            ex("3 6\n3 2 4\n", "1 2", "nums[1] + nums[2] = 6")
                    ),
                    cases(
                            tc("4 9\n2 7 11 15\n", "0 1"),
                            tc("3 6\n3 2 4\n", "1 2"),
                            hidden("2 6\n3 3\n", "0 1"),
                            hidden("5 10\n1 8 3 7 5\n", "2 3")
                    )
            ),
            new Problem(
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
                            ex("()[]{}\n", "true", "每个左括号都由相同类型的右括号闭合。"),
                            ex("(]\n", "false", "括号类型不匹配。")
                    ),
                    cases(
                            tc("()[]{}\n", "true"),
                            tc("(]\n", "false"),
                            hidden("([{}])\n", "true"),
                            hidden("(((()))\n", "false")
                    )
            ),
            new Problem(
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
                            ex("8\n10 9 2 5 3 7 101 18\n", "4", "其中一个最长递增子序列是 2, 3, 7, 101。"),
                            ex("6\n0 1 0 3 2 3\n", "4", "其中一个最长递增子序列是 0, 1, 2, 3。")
                    ),
                    cases(
                            tc("8\n10 9 2 5 3 7 101 18\n", "4"),
                            tc("6\n0 1 0 3 2 3\n", "4"),
                            hidden("7\n7 7 7 7 7 7 7\n", "1"),
                            hidden("9\n4 10 4 3 8 9 11 2 12\n", "5")
                    )
            ),
            new Problem(
                    4,
                    "最大子数组和",
                    "简单",
                    List.of("数组", "动态规划"),
                    "给定整数数组 nums，找到一个连续子数组，使其元素和最大，并输出最大和。",
                    "第一行包含 n。第二行包含 n 个整数。",
                    "输出最大连续子数组和。",
                    "1 <= n <= 100000, -100000 <= nums[i] <= 100000",
                    basicArrayStarter("// TODO: 计算最大连续子数组和\n        System.out.println(0);"),
                    List.of(
                            ex("9\n-2 1 -3 4 -1 2 1 -5 4\n", "6", "连续子数组 4, -1, 2, 1 的和最大。"),
                            ex("5\n5 4 -1 7 8\n", "23", "整个数组的和最大。")
                    ),
                    cases(
                            tc("9\n-2 1 -3 4 -1 2 1 -5 4\n", "6"),
                            tc("5\n5 4 -1 7 8\n", "23"),
                            hidden("1\n-5\n", "-5"),
                            hidden("4\n-1 -2 -3 -4\n", "-1")
                    )
            ),
            new Problem(
                    5,
                    "二分查找",
                    "简单",
                    List.of("数组", "二分查找"),
                    "给定升序整数数组 nums 和目标值 target，返回 target 的下标；如果不存在，返回 -1。",
                    "第一行包含 n 和 target。第二行包含 n 个升序整数。",
                    "输出 target 的下标或 -1。",
                    "1 <= n <= 100000, -1000000000 <= nums[i], target <= 1000000000",
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

                                    // TODO: 使用二分查找输出下标
                                    System.out.println(-1);
                                }
                            }
                            """,
                    List.of(
                            ex("5 9\n-1 0 3 5 9\n", "4", "9 位于下标 4。"),
                            ex("6 2\n-1 0 3 5 9 12\n", "-1", "数组中不存在 2。")
                    ),
                    cases(
                            tc("5 9\n-1 0 3 5 9\n", "4"),
                            tc("6 2\n-1 0 3 5 9 12\n", "-1"),
                            hidden("1 7\n7\n", "0"),
                            hidden("4 -3\n-5 -3 0 2\n", "1")
                    )
            ),
            new Problem(
                    6,
                    "合并区间",
                    "中等",
                    List.of("数组", "排序"),
                    "给定若干闭区间，请合并所有重叠区间，并按左端点升序输出。",
                    "第一行包含 n。接下来 n 行每行包含 l 和 r。",
                    "每行输出一个合并后的区间 l r，按左端点升序排列。",
                    "1 <= n <= 100000, -1000000000 <= l <= r <= 1000000000",
                    """
                            import java.util.*;

                            public class Main {
                                public static void main(String[] args) {
                                    Scanner scanner = new Scanner(System.in);
                                    int n = scanner.nextInt();
                                    int[][] intervals = new int[n][2];
                                    for (int i = 0; i < n; i++) {
                                        intervals[i][0] = scanner.nextInt();
                                        intervals[i][1] = scanner.nextInt();
                                    }

                                    // TODO: 合并并输出区间
                                }
                            }
                            """,
                    List.of(
                            ex("4\n1 3\n2 6\n8 10\n15 18\n", "1 6\n8 10\n15 18", "区间 [1,3] 与 [2,6] 重叠。"),
                            ex("2\n1 4\n4 5\n", "1 5", "端点相接也视为可合并。")
                    ),
                    cases(
                            tc("4\n1 3\n2 6\n8 10\n15 18\n", "1 6\n8 10\n15 18"),
                            tc("2\n1 4\n4 5\n", "1 5"),
                            hidden("4\n1 4\n4 5\n2 3\n8 9\n", "1 5\n8 9"),
                            hidden("3\n5 7\n1 2\n3 4\n", "1 2\n3 4\n5 7")
                    )
            ),
            new Problem(
                    7,
                    "爬楼梯",
                    "简单",
                    List.of("动态规划", "数学"),
                    "每次可以爬 1 或 2 阶。给定楼梯阶数 n，输出到达楼顶的不同方法数。",
                    "一行一个整数 n。",
                    "输出方法数。",
                    "1 <= n <= 45",
                    singleIntStarter("// TODO: 输出爬到第 n 阶的方法数\n        System.out.println(0);"),
                    List.of(
                            ex("2\n", "2", "可以走 1+1 或 2。"),
                            ex("3\n", "3", "可以走 1+1+1、1+2、2+1。")
                    ),
                    cases(
                            tc("2\n", "2"),
                            tc("3\n", "3"),
                            hidden("1\n", "1"),
                            hidden("10\n", "89")
                    )
            ),
            new Problem(
                    8,
                    "买卖股票最佳时机",
                    "简单",
                    List.of("数组", "贪心"),
                    "给定每天的股票价格，只能先买入一次再卖出一次，输出可以获得的最大利润。",
                    "第一行包含 n。第二行包含 n 个价格。",
                    "输出最大利润；如果无法获利，输出 0。",
                    "1 <= n <= 100000, 0 <= prices[i] <= 100000",
                    basicArrayStarter("// TODO: 输出一次买卖可获得的最大利润\n        System.out.println(0);"),
                    List.of(
                            ex("6\n7 1 5 3 6 4\n", "5", "第 2 天买入，第 5 天卖出。"),
                            ex("5\n7 6 4 3 1\n", "0", "价格持续下降，无法获利。")
                    ),
                    cases(
                            tc("6\n7 1 5 3 6 4\n", "5"),
                            tc("5\n7 6 4 3 1\n", "0"),
                            hidden("2\n1 2\n", "1"),
                            hidden("6\n2 4 1 7 5 9\n", "8")
                    )
            ),
            new Problem(
                    9,
                    "移动零",
                    "简单",
                    List.of("数组", "双指针"),
                    "给定整数数组 nums，将所有 0 移动到数组末尾，同时保持非零元素的相对顺序。",
                    "第一行包含 n。第二行包含 n 个整数。",
                    "输出移动后的数组，元素之间用空格分隔。",
                    "1 <= n <= 100000, -1000000000 <= nums[i] <= 1000000000",
                    basicArrayStarter("// TODO: 原地或辅助数组移动零并输出\n        System.out.println();"),
                    List.of(
                            ex("5\n0 1 0 3 12\n", "1 3 12 0 0", "非零元素 1, 3, 12 保持原顺序。"),
                            ex("3\n0 0 1\n", "1 0 0", "所有 0 移到末尾。")
                    ),
                    cases(
                            tc("5\n0 1 0 3 12\n", "1 3 12 0 0"),
                            tc("3\n0 0 1\n", "1 0 0"),
                            hidden("1\n0\n", "0"),
                            hidden("6\n4 0 5 0 0 6\n", "4 5 6 0 0 0")
                    )
            ),
            new Problem(
                    10,
                    "岛屿数量",
                    "中等",
                    List.of("图", "DFS", "BFS"),
                    "给定由 0 和 1 组成的网格，1 表示陆地，0 表示水域。上下左右相邻的陆地属于同一座岛屿，输出岛屿数量。",
                    "第一行包含 m 和 n。接下来 m 行每行是长度为 n 的 01 字符串。",
                    "输出岛屿数量。",
                    "1 <= m, n <= 300",
                    """
                            import java.util.*;

                            public class Main {
                                public static void main(String[] args) {
                                    Scanner scanner = new Scanner(System.in);
                                    int m = scanner.nextInt();
                                    int n = scanner.nextInt();
                                    char[][] grid = new char[m][n];
                                    for (int i = 0; i < m; i++) {
                                        grid[i] = scanner.next().toCharArray();
                                    }

                                    // TODO: 统计岛屿数量
                                    System.out.println(0);
                                }
                            }
                            """,
                    List.of(
                            ex("4 5\n11110\n11010\n11000\n00000\n", "1", "所有陆地连成一座岛屿。"),
                            ex("4 5\n11000\n11000\n00100\n00011\n", "3", "网格中有三座岛屿。")
                    ),
                    cases(
                            tc("4 5\n11110\n11010\n11000\n00000\n", "1"),
                            tc("4 5\n11000\n11000\n00100\n00011\n", "3"),
                            hidden("1 1\n1\n", "1"),
                            hidden("3 3\n010\n101\n010\n", "4")
                    )
            ),
            new Problem(
                    11,
                    "最长无重复子串",
                    "中等",
                    List.of("字符串", "滑动窗口"),
                    "给定字符串 s，输出不含重复字符的最长连续子串长度。",
                    "一行字符串 s。",
                    "输出最长长度。",
                    "0 <= s.length <= 50000",
                    stringStarter("// TODO: 输出最长无重复子串长度\n        System.out.println(0);"),
                    List.of(
                            ex("abcabcbb\n", "3", "最长子串为 abc。"),
                            ex("bbbbb\n", "1", "最长子串为 b。")
                    ),
                    cases(
                            tc("abcabcbb\n", "3"),
                            tc("bbbbb\n", "1"),
                            hidden("pwwkew\n", "3"),
                            hidden("dvdf\n", "3")
                    )
            ),
            new Problem(
                    12,
                    "有效的字母异位词",
                    "简单",
                    List.of("字符串", "计数"),
                    "给定两个仅包含小写字母的字符串 s 和 t，判断 t 是否是 s 的字母异位词。",
                    "第一行是 s。第二行是 t。",
                    "如果是字母异位词，输出 true；否则输出 false。",
                    "1 <= s.length, t.length <= 50000",
                    """
                            import java.util.*;

                            public class Main {
                                public static void main(String[] args) {
                                    Scanner scanner = new Scanner(System.in);
                                    String s = scanner.nextLine();
                                    String t = scanner.nextLine();

                                    // TODO: 判断两个字符串是否互为字母异位词
                                    System.out.println(false);
                                }
                            }
                            """,
                    List.of(
                            ex("anagram\nnagaram\n", "true", "两个字符串中每个字母出现次数相同。"),
                            ex("rat\ncar\n", "false", "字母计数不同。")
                    ),
                    cases(
                            tc("anagram\nnagaram\n", "true"),
                            tc("rat\ncar\n", "false"),
                            hidden("a\nab\n", "false"),
                            hidden("listen\nsilent\n", "true")
                    )
            ),
            new Problem(
                    13,
                    "接雨水",
                    "困难",
                    List.of("数组", "双指针", "单调栈"),
                    "给定 n 个非负整数表示柱子高度，计算下雨后可以接住多少单位的雨水。",
                    "第一行包含 n。第二行包含 n 个非负整数。",
                    "输出可接雨水总量。",
                    "1 <= n <= 100000, 0 <= height[i] <= 100000",
                    basicArrayStarter("// TODO: 输出可接住的雨水总量\n        System.out.println(0);"),
                    List.of(
                            ex("12\n0 1 0 2 1 0 1 3 2 1 2 1\n", "6", "经典示例，可接 6 单位雨水。"),
                            ex("6\n4 2 0 3 2 5\n", "9", "左右边界共同决定蓄水量。")
                    ),
                    cases(
                            tc("12\n0 1 0 2 1 0 1 3 2 1 2 1\n", "6"),
                            tc("6\n4 2 0 3 2 5\n", "9"),
                            hidden("3\n1 2 3\n", "0"),
                            hidden("5\n5 4 1 2 6\n", "8")
                    )
            ),
            new Problem(
                    14,
                    "零钱兑换",
                    "中等",
                    List.of("动态规划", "背包"),
                    "给定若干硬币面额和总金额 amount，输出凑成该金额所需的最少硬币数。如果无法凑成，输出 -1。",
                    "第一行包含 n 和 amount。第二行包含 n 个硬币面额。",
                    "输出最少硬币数或 -1。",
                    "1 <= n <= 20, 0 <= amount <= 10000",
                    """
                            import java.util.*;

                            public class Main {
                                public static void main(String[] args) {
                                    Scanner scanner = new Scanner(System.in);
                                    int n = scanner.nextInt();
                                    int amount = scanner.nextInt();
                                    int[] coins = new int[n];
                                    for (int i = 0; i < n; i++) {
                                        coins[i] = scanner.nextInt();
                                    }

                                    // TODO: 输出最少硬币数
                                    System.out.println(-1);
                                }
                            }
                            """,
                    List.of(
                            ex("3 11\n1 2 5\n", "3", "11 = 5 + 5 + 1。"),
                            ex("1 3\n2\n", "-1", "无法用面额 2 凑出 3。")
                    ),
                    cases(
                            tc("3 11\n1 2 5\n", "3"),
                            tc("1 3\n2\n", "-1"),
                            hidden("3 0\n1 2 5\n", "0"),
                            hidden("4 27\n2 5 10 1\n", "4")
                    )
            ),
            new Problem(
                    15,
                    "最小路径和",
                    "中等",
                    List.of("动态规划", "矩阵"),
                    "给定一个非负整数矩阵，从左上角出发，每次只能向右或向下移动，输出到右下角路径上的最小数字和。",
                    "第一行包含 m 和 n。接下来 m 行每行包含 n 个整数。",
                    "输出最小路径和。",
                    "1 <= m, n <= 200, 0 <= grid[i][j] <= 10000",
                    """
                            import java.util.*;

                            public class Main {
                                public static void main(String[] args) {
                                    Scanner scanner = new Scanner(System.in);
                                    int m = scanner.nextInt();
                                    int n = scanner.nextInt();
                                    int[][] grid = new int[m][n];
                                    for (int i = 0; i < m; i++) {
                                        for (int j = 0; j < n; j++) {
                                            grid[i][j] = scanner.nextInt();
                                        }
                                    }

                                    // TODO: 输出最小路径和
                                    System.out.println(0);
                                }
                            }
                            """,
                    List.of(
                            ex("3 3\n1 3 1\n1 5 1\n4 2 1\n", "7", "路径 1->3->1->1->1 的和最小。"),
                            ex("2 3\n1 2 3\n4 5 6\n", "12", "最小路径和为 12。")
                    ),
                    cases(
                            tc("3 3\n1 3 1\n1 5 1\n4 2 1\n", "7"),
                            tc("2 3\n1 2 3\n4 5 6\n", "12"),
                            hidden("1 1\n5\n", "5"),
                            hidden("3 2\n1 2\n1 1\n9 1\n", "4")
                    )
            ),
            new Problem(
                    16,
                    "区间和查询",
                    "简单",
                    List.of("数组", "前缀和"),
                    "给定整数数组和多次查询，每次查询闭区间 [l, r] 的元素和。下标从 0 开始。",
                    "第一行包含 n 和 q。第二行包含 n 个整数。接下来 q 行每行包含 l 和 r。",
                    "每个查询输出一行区间和。",
                    "1 <= n, q <= 100000, -100000 <= nums[i] <= 100000",
                    """
                            import java.util.*;

                            public class Main {
                                public static void main(String[] args) {
                                    Scanner scanner = new Scanner(System.in);
                                    int n = scanner.nextInt();
                                    int q = scanner.nextInt();
                                    int[] nums = new int[n];
                                    for (int i = 0; i < n; i++) {
                                        nums[i] = scanner.nextInt();
                                    }

                                    // TODO: 使用前缀和回答 q 次查询
                                }
                            }
                            """,
                    List.of(
                            ex("5 3\n1 2 3 4 5\n0 2\n1 3\n2 4\n", "6\n9\n12", "三个查询分别求 [0,2]、[1,3]、[2,4]。"),
                            ex("3 2\n-1 2 7\n0 0\n0 2\n", "-1\n8", "区间可以只包含一个元素。")
                    ),
                    cases(
                            tc("5 3\n1 2 3 4 5\n0 2\n1 3\n2 4\n", "6\n9\n12"),
                            tc("3 2\n-1 2 7\n0 0\n0 2\n", "-1\n8"),
                            hidden("1 1\n42\n0 0\n", "42"),
                            hidden("4 2\n5 -2 3 1\n1 2\n0 3\n", "1\n7")
                    )
            ),
            new Problem(
                    17,
                    "课程表",
                    "中等",
                    List.of("图", "拓扑排序"),
                    "有 n 门课程和若干先修关系。每个关系 a b 表示想学习课程 a，必须先学习课程 b。判断是否可以完成所有课程。",
                    "第一行包含 n 和 m。接下来 m 行每行包含 a 和 b。",
                    "如果可以完成全部课程，输出 true；否则输出 false。",
                    "1 <= n <= 100000, 0 <= m <= 200000",
                    """
                            import java.util.*;

                            public class Main {
                                public static void main(String[] args) {
                                    Scanner scanner = new Scanner(System.in);
                                    int n = scanner.nextInt();
                                    int m = scanner.nextInt();

                                    // TODO: 建图并判断是否存在环
                                    System.out.println(false);
                                }
                            }
                            """,
                    List.of(
                            ex("2 1\n1 0\n", "true", "先学 0 再学 1。"),
                            ex("2 2\n1 0\n0 1\n", "false", "两门课互相依赖，形成环。")
                    ),
                    cases(
                            tc("2 1\n1 0\n", "true"),
                            tc("2 2\n1 0\n0 1\n", "false"),
                            hidden("4 3\n1 0\n2 1\n3 2\n", "true"),
                            hidden("3 3\n0 1\n1 2\n2 0\n", "false")
                    )
            ),
            new Problem(
                    18,
                    "滑动窗口最大值",
                    "困难",
                    List.of("队列", "滑动窗口"),
                    "给定整数数组 nums 和窗口大小 k，窗口从左到右每次移动一位，输出每个窗口中的最大值。",
                    "第一行包含 n 和 k。第二行包含 n 个整数。",
                    "输出所有窗口最大值，元素之间用空格分隔。",
                    "1 <= k <= n <= 100000",
                    """
                            import java.util.*;

                            public class Main {
                                public static void main(String[] args) {
                                    Scanner scanner = new Scanner(System.in);
                                    int n = scanner.nextInt();
                                    int k = scanner.nextInt();
                                    int[] nums = new int[n];
                                    for (int i = 0; i < n; i++) {
                                        nums[i] = scanner.nextInt();
                                    }

                                    // TODO: 使用单调队列输出每个窗口最大值
                                    System.out.println();
                                }
                            }
                            """,
                    List.of(
                            ex("8 3\n1 3 -1 -3 5 3 6 7\n", "3 3 5 5 6 7", "窗口最大值依次为 3, 3, 5, 5, 6, 7。"),
                            ex("1 1\n1\n", "1", "只有一个窗口。")
                    ),
                    cases(
                            tc("8 3\n1 3 -1 -3 5 3 6 7\n", "3 3 5 5 6 7"),
                            tc("1 1\n1\n", "1"),
                            hidden("5 2\n9 8 7 6 5\n", "9 8 7 6"),
                            hidden("6 4\n4 2 12 3 8 7\n", "12 12 12")
                    )
            ),
            new Problem(
                    19,
                    "编辑距离",
                    "困难",
                    List.of("字符串", "动态规划"),
                    "给定两个单词 word1 和 word2，计算将 word1 转换成 word2 所需的最少操作数。允许插入、删除、替换一个字符。",
                    "第一行是 word1。第二行是 word2。",
                    "输出最少操作数。",
                    "0 <= word1.length, word2.length <= 500",
                    """
                            import java.util.*;

                            public class Main {
                                public static void main(String[] args) {
                                    Scanner scanner = new Scanner(System.in);
                                    String word1 = scanner.nextLine();
                                    String word2 = scanner.nextLine();

                                    // TODO: 输出编辑距离
                                    System.out.println(0);
                                }
                            }
                            """,
                    List.of(
                            ex("horse\nros\n", "3", "horse -> rorse -> rose -> ros。"),
                            ex("intention\nexecution\n", "5", "最少需要 5 次操作。")
                    ),
                    cases(
                            tc("horse\nros\n", "3"),
                            tc("intention\nexecution\n", "5"),
                            hidden("a\n\n", "1"),
                            hidden("kitten\nsitting\n", "3")
                    )
            ),
            new Problem(
                    20,
                    "最长公共子序列",
                    "中等",
                    List.of("字符串", "动态规划"),
                    "给定两个字符串 text1 和 text2，输出它们最长公共子序列的长度。子序列不要求连续，但相对顺序必须保持。",
                    "第一行是 text1。第二行是 text2。",
                    "输出最长公共子序列长度。",
                    "1 <= text1.length, text2.length <= 1000",
                    """
                            import java.util.*;

                            public class Main {
                                public static void main(String[] args) {
                                    Scanner scanner = new Scanner(System.in);
                                    String text1 = scanner.nextLine();
                                    String text2 = scanner.nextLine();

                                    // TODO: 输出最长公共子序列长度
                                    System.out.println(0);
                                }
                            }
                            """,
                    List.of(
                            ex("abcde\nace\n", "3", "最长公共子序列为 ace。"),
                            ex("abc\ndef\n", "0", "没有公共字符。")
                    ),
                    cases(
                            tc("abcde\nace\n", "3"),
                            tc("abc\ndef\n", "0"),
                            hidden("abc\nabc\n", "3"),
                            hidden("bsbininm\njmjkbkjkv\n", "1")
                    )
            )
    ).stream().collect(Collectors.toUnmodifiableMap(Problem::id, Function.identity()));

    public Collection<Problem> findAll() {
        return problems.values();
    }

    public Optional<Problem> findById(long id) {
        return Optional.ofNullable(problems.get(id));
    }

    private static ExampleCase ex(String input, String output, String explanation) {
        return new ExampleCase(input, output, explanation);
    }

    private static List<TestCase> cases(TestCase... testCases) {
        return List.of(testCases);
    }

    private static TestCase tc(String input, String expectedOutput) {
        return new TestCase(input, expectedOutput, false);
    }

    private static TestCase hidden(String input, String expectedOutput) {
        return new TestCase(input, expectedOutput, true);
    }

    private static String basicArrayStarter(String body) {
        return """
                import java.util.*;

                public class Main {
                    public static void main(String[] args) {
                        Scanner scanner = new Scanner(System.in);
                        int n = scanner.nextInt();
                        int[] nums = new int[n];
                        for (int i = 0; i < n; i++) {
                            nums[i] = scanner.nextInt();
                        }

                %s
                    }
                }
                """.formatted(body);
    }

    private static String singleIntStarter(String body) {
        return """
                import java.util.*;

                public class Main {
                    public static void main(String[] args) {
                        Scanner scanner = new Scanner(System.in);
                        int n = scanner.nextInt();

                %s
                    }
                }
                """.formatted(body);
    }

    private static String stringStarter(String body) {
        return """
                import java.util.*;

                public class Main {
                    public static void main(String[] args) {
                        Scanner scanner = new Scanner(System.in);
                        String s = scanner.nextLine();

                %s
                    }
                }
                """.formatted(body);
    }
}
