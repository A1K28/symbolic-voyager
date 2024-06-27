package com.github.a1k28.evoc.core.asm;

import com.github.a1k28.evoc.model.branch.BranchDistanceData;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.util.HashMap;
import java.util.Map;

/**
 * <h2>Branch distance calculation from Tracey et al.</h2>
 * <h4>(I have corrected some of them myself, as there were errors)</h4>
 * <table border="1">
 *   <thead>
 *     <tr>
 *       <th>Element</th>
 *       <th>Value</th>
 *     </tr>
 *   </thead>
 *   <tbody>
 *     <tr>
 *       <td>Boolean</td>
 *       <td>if TRUE then 0 else K</td>
 *     </tr>
 *     <tr>
 *       <td>a = b</td>
 *       <td>if abs(a-b) = 0 then 0 else abs(a-b) + K</td>
 *     </tr>
 *     <tr>
 *       <td>a ≠ b</td>
 *       <td>if abs(a-b) ≠ 0 then 0 else K</td>
 *     </tr>
 *     <tr>
 *       <td>a < b</td>
 *       <td>if a-b < 0 then 0 else (a-b) + K</td>
 *     </tr>
 *     <tr>
 *       <td>a ≤ b</td>
 *       <td>if a-b ≤ 0 then 0 else (a-b) + K</td>
 *     </tr>
 *     <tr>
 *       <td>a > b</td>
 *       <td>if a-b > 0 then 0 else (b-a) + K</td>
 *     </tr>
 *     <tr>
 *       <td>a ≥ b</td>
 *       <td>if a-b ≥ 0 then 0 else (b-a) + K</td>
 *     </tr>
 *     <tr>
 *       <td>a ∨ b</td>
 *       <td>min (cost (a), cost (b))</td>
 *     </tr>
 *     <tr>
 *       <td>a ∧ b</td>
 *       <td>cost (a) + cost (b)</td>
 *     </tr>
 *     <tr>
 *       <td>¬ a</td>
 *       <td>Negation is moved inwards and propagated over a</td>
 *     </tr>
 *   </tbody>
 * </table>
 *
 * <p>Retrieved from: https://www.duo.uio.no/bitstream/handle/10852/8791/Jansen_Master.pdf</p>
 * <p>Oracle JVM instruction set: https://docs.oracle.com/javase/specs/jvms/se7/html/jvms-6.html</p>
 */
public class ASMConditionalWrapper {
    static String testId;
    private final static Integer K = 10;
    final static Map<String, BranchDistanceData> distanceData = new HashMap<>();

    public static boolean IF_ICMPEQ(int a, int b, String bId) {
        int score = Math.abs(a-b);
        score = score != 0 ? 0 : K;
        put(bId, (long) score);
        return a != b;
    }

    public static boolean IF_ICMPNE(int a, int b, String bId) {
        int score = Math.abs(a-b);
        score = score == 0 ? 0 : score + K;
        put(bId, (long) score);
        return a == b;
    }

    public static boolean IF_ICMPLT(int a, int b, String bId) {
        int score = a - b >= 0 ? 0 : (b - a) + K;
        put(bId, (long) score);
        return a >= b;
    }

    public static boolean IF_ICMPLE(int a, int b, String bId) {
        int score = a - b > 0 ? 0 : (b - a) + K;
        put(bId, (long) score);
        return a > b;
    }

    public static boolean IF_ICMPGT(int a, int b, String bId) {
        int score = a - b;
        score = score <= 0 ? 0 : score + K;
        put(bId, (long) score);
        return a <= b;
    }

    public static boolean IF_ICMPGE(int a, int b, String bId) {
        int score = a - b;
        score = score < 0 ? 0 : score + K;
        put(bId, (long) score);
        return a < b;
    }

    public static boolean IFEQ_INT(int a, int b, String bId) {
        int score = Math.abs(a-b);
        score = score != 0 ? 0 : K;
        put(bId, (long) score);
        return a != b;
    }

    public static boolean IFNE_INT(int a, int b, String bId) {
        int score = Math.abs(a-b);
        score = score == 0 ? 0 : score + K;
        put(bId, (long) score);
        return a == b;
    }

    public static boolean IFLT_INT(int a, int b, String bId) {
        int score = a - b >= 0 ? 0 : (b - a) + K;
        put(bId, (long) score);
        return a >= b;
    }

    public static boolean IFLE_INT(int a, int b, String bId) {
        int score = a - b > 0 ? 0 : (b - a) + K;
        put(bId, (long) score);
        return a > b;
    }

    public static boolean IFGT_INT(int a, int b, String bId) {
        int score = a - b;
        score = score <= 0 ? 0 : score + K;
        put(bId, (long) score);
        return a <= b;
    }

    public static boolean IFGE_INT(int a, int b, String bId) {
        int score = a - b;
        score = score < 0 ? 0 : score + K;
        put(bId, (long) score);
        return a < b;
    }

    public static boolean IFEQ_LONG(long a, long b, String bId) {
        long score = Math.abs(a-b);
        score = score != 0 ? 0 : K;
        put(bId, score);
        return a != b;
    }

    public static boolean IFNE_LONG(long a, long b, String bId) {
        long score = Math.abs(a-b);
        score = score == 0 ? 0 : score + K;
        put(bId, score);
        return a == b;
    }

    public static boolean IFLT_LONG(long a, long b, String bId) {
        long score = a - b >= 0 ? 0 : (b - a) + K;
        put(bId, score);
        return a >= b;
    }

    public static boolean IFLE_LONG(long a, long b, String bId) {
        long score = a - b > 0 ? 0 : (b - a) + K;
        put(bId, score);
        return a > b;
    }

    public static boolean IFGT_LONG(long a, long b, String bId) {
        long score = a - b;
        score = score <= 0 ? 0 : score + K;
        put(bId, score);
        return a <= b;
    }

    public static boolean IFGE_LONG(long a, long b, String bId) {
        long score = a - b;
        score = score < 0 ? 0 : score + K;
        put(bId, score);
        return a < b;
    }

    public static boolean IFEQ_FLOAT(float a, float b, String bId) {
        float score = Math.abs(a-b);
        score = score != 0 ? 0 : K;
        put(bId, (long) score);
        return a != b;
    }

    public static boolean IFNE_FLOAT(float a, float b, String bId) {
        float score = Math.abs(a-b);
        score = score == 0 ? 0 : score + K;
        put(bId, (long) score);
        return a == b;
    }

    public static boolean IFLT_FLOAT(float a, float b, String bId) {
        float score = a - b >= 0 ? 0 : (b - a) + K;
        put(bId, (long) score);
        return a >= b;
    }

    public static boolean IFLE_FLOAT(float a, float b, String bId) {
        float score = a - b > 0 ? 0 : (b - a) + K;
        put(bId, (long) score);
        return a > b;
    }

    public static boolean IFGT_FLOAT(float a, float b, String bId) {
        float score = a - b;
        score = score <= 0 ? 0 : score + K;
        put(bId, (long) score);
        return a <= b;
    }

    public static boolean IFGE_FLOAT(float a, float b, String bId) {
        float score = a - b;
        score = score < 0 ? 0 : score + K;
        put(bId, (long) score);
        return a < b;
    }

    public static boolean IFEQ_DOUBLE(double a, double b, String bId) {
        double score = Math.abs(a-b);
        score = score != 0 ? 0 : K;
        put(bId, (long) score);
        return a != b;
    }

    public static boolean IFNE_DOUBLE(double a, double b, String bId) {
        double score = Math.abs(a-b);
        score = score == 0 ? 0 : score + K;
        put(bId, (long) score);
        return a == b;
    }

    public static boolean IFLT_DOUBLE(double a, double b, String bId) {
        double score = a - b >= 0 ? 0 : (b - a) + K;
        put(bId, (long) score);
        return a >= b;
    }

    public static boolean IFLE_DOUBLE(double a, double b, String bId) {
        double score = a - b > 0 ? 0 : (b - a) + K;
        put(bId, (long) score);
        return a > b;
    }

    public static boolean IFGT_DOUBLE(double a, double b, String bId) {
        double score = a - b;
        score = score <= 0 ? 0 : score + K;
        put(bId, (long) score);
        return a <= b;
    }

    public static boolean IFGE_DOUBLE(double a, double b, String bId) {
        double score = a - b;
        score = score < 0 ? 0 : score + K;
        put(bId, (long) score);
        return a < b;
    }

    public static boolean IFEQ_STRING(String a, String b, String bId) {
        long dist = LevenshteinDistance.getDefaultInstance().apply(a, b).longValue();
        dist = dist == 0 ? 0 : dist + K;
        put(bId, dist);
        return a.equals(b);
    }

    public static boolean IFNE_STRING(String a, String b, String bId) {
        long dist = LevenshteinDistance.getDefaultInstance().apply(a, b).longValue();
        dist = dist == 0 ? 0 : dist + K;
        float norm = 1 - normalize(dist);
        putNormalized(bId, norm);
        return a.equals(b);
    }

    public static void clear() {
        ASMConditionalWrapper.testId = null;
        ASMConditionalWrapper.distanceData.clear();
    }

    private static void put(String bId, Long score) {
        if (!distanceData.containsKey(testId)) {
            BranchDistanceData data = new BranchDistanceData(testId);
            distanceData.put(testId, data);
        }
        distanceData.get(testId).getDistances().put(bId, normalize(score));
    }

    private static void putNormalized(String bId, float score) {
        if (!distanceData.containsKey(testId)) {
            BranchDistanceData data = new BranchDistanceData(testId);
            distanceData.put(testId, data);
        }
        distanceData.get(testId).getDistances().put(bId, score);
    }

    private static float normalize(Long dist) {
        return (float) (1. - Math.pow(1.001, -dist));
    }
}
