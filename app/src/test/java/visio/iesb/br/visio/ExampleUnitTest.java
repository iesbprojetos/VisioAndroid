package visio.iesb.br.visio;

import org.junit.Test;

import java.util.Arrays;

import Jama.Matrix;
import Jama.SingularValueDecomposition;

import static org.junit.Assert.*;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void testJamaSVD() throws Exception {
        double[][] a = new double[][] {
                {3, 1, 1},
                {-1, 3, 1}
        };

        double[][] u = new double[][] {
                {1.0 / Math.sqrt(2), 1.0 / Math.sqrt(2)},
                {1.0 / Math.sqrt(2), -1.0 / Math.sqrt(2)}
        };

        double[][] s = new double[][] {
                {Math.sqrt(12), 0, 0},
                {0, Math.sqrt(10), 0}
        };

        double[][] v = new double[][] {
                {1/Math.sqrt(6), 2/Math.sqrt(6), 1/Math.sqrt(6)},
                {2/Math.sqrt(5), -1/Math.sqrt(5), 0},
                {1/Math.sqrt(30), 2/Math.sqrt(30), -5/Math.sqrt(30)},
        };

        Matrix matrixA = new Matrix(a);
        Matrix matrixU = new Matrix(u);
        Matrix matrixS = new Matrix(s);
        Matrix matrixV = new Matrix(v);

        SingularValueDecomposition svd = matrixA.svd();
        Matrix left = svd.getU();
        Matrix singular = svd.getS();
        Matrix right = svd.getV();

        assertTrue(Arrays.equals(matrixU.getArray(), left.getArray()));
        assertTrue(Arrays.equals(matrixS.getArray(), singular.getArray()));
        assertTrue(Arrays.equals(matrixV.getArray(), right.getArray()));

        Matrix matrixOriginal = matrixU.times(matrixS).times(matrixV.transpose());
        assertTrue(Arrays.equals(matrixA.getArray(), matrixOriginal.getArray()));
    }
}