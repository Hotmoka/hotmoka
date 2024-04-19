package io.hotmoka.tests;

import static io.hotmoka.crypto.Base64.toBase64String;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.hotmoka.beans.ConstructorSignatures;
import io.hotmoka.beans.FieldSignatures;
import io.hotmoka.beans.MethodSignatures;
import io.hotmoka.beans.StorageTypes;
import io.hotmoka.beans.StorageValues;
import io.hotmoka.beans.TransactionReferences;
import io.hotmoka.beans.api.signatures.FieldSignature;
import io.hotmoka.beans.api.transactions.TransactionReference;
import io.hotmoka.beans.api.values.StorageReference;
import io.hotmoka.beans.api.values.StorageValue;
import io.hotmoka.beans.internal.marshalling.NodeMarshallingContext;
import io.hotmoka.marshalling.MarshallingContexts;
import io.hotmoka.node.TransactionRequests;

public class Marshallable {

    @Test
    @DisplayName("write(bytesOf(\"lambdas.jar\"))")
    public void testJarBytes() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = MarshallingContexts.of(baos)) {
            context.writeBytes(bytesOf("lambdas.jar"));
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXoAAAQAUEsDBBQAAAgIAEt5elMKW93AUAAAAFEAAAAUAAAATUVUQS1JTkYvTUFOSUZFU1QuTUbzTczLTEstLtENSy0qzszPs1Iw1DPg5XIuSk0sSU3Rdaq0UvBNLEvNU/BKLFIIyClNz8xTMNYzAqlxKs3MSdH1SsnWDS5ITQZqNOTl4uUCAFBLAwQKAAAIAABLeXpTAAAAAAAAAAAAAAAACQAAAE1FVEEtSU5GL1BLAwQKAAAIAABLeXpTAAAAAAAAAAAAAAAAAwAAAGlvL1BLAwQKAAAIAABLeXpTAAAAAAAAAAAAAAAACwAAAGlvL2hvdG1va2EvUEsDBAoAAAgAAEt5elMAAAAAAAAAAAAAAAAUAAAAaW8vaG90bW9rYS9leGFtcGxlcy9QSwMECgAACAAASnl6UwAAAAAAAAAAAAAAABwAAABpby9ob3Rtb2thL2V4YW1wbGVzL2xhbWJkYXMvUEsDBBQAAAgIAEp5elNK4TRKwQsAAKQgAAApAAAAaW8vaG90bW9rYS9leGFtcGxlcy9sYW1iZGFzL0xhbWJkYXMuY2xhc3O1WQd8G9UZ/z/Z1tnyOVJEBnGIMzCJLDtxnAlkEMc4iRPHTmMnwVCgZ/lsXyzdGekUSGmhLbSslu6W7l066AglyxRKd0vp3ovuvQfdpfzf3fmsyJIsRu2fT9+9977v+3/jve978oOPTtwPYKPYGMJ63FwF52dFJISFuKUGYdyq4LYQgnihfHmRfNwuHy8O8fESBS+txcvw8mq8QsEra8jzKjn+ajl4R5SC3liLN+HNUQi8Vc6+LYoA3hFFBe6MolKOhfFuBe+Rq94bwl14XxRVOCZf745S6QlJnazFKZyWIieiUHBfFNV4IIoa+Qjhzlp8HJ+QWj+p4FNR1OKBED6LByXn5xQ8FMIqfD4KFV8K8fFlufArUdThmKS+ruAbAtG9XT1dew/svbKr52BnX//ezp5+gXndh7UjWmtKs0dbtxsjXaatj+jpTQI1hnlEz9hWOiNwbrdhtdramJbiX2vCGtJbs7aRbO3jtDaidxsZW3L0GSOmZmfTukD3zBybpy9JauZI6z7tqDaY1Dss005rCXvTVoqusuxRPS1wnuQZteyURRb9Gi01ntQzZEsNDmmZ1m73k+uDWsrKmjYxjWcHk0Zij36U9rumOkr67LRhjsiVmw3TsLcKNMcKe2I6U9NBgcoO4hUIdxum3pNNDerpfglaKrESWvKgljbkuzdYaY8adOOC/cRkpPSDRsbgeLtpWrZmG5bJucWlvUGkjUVW7EhbKd9ZVFLEDok66MaUhOPPNXTsES2ZJcIVZQZDYK5NCa6nDxn2qJW1+x3jKmJSQ4VlUlpDwTU7dXoAAAQAu0/am6CXzlzgSqiXg3t1rh/arw/rad1M6P1WJzUfdcR3CSwvvqR3uFfa1JHUMpRVPZm9JXn6tJRvGkMgF/I1Y6ezCbJOBxGbYcU+a3y/nskmZeKN6Ha7l4Rnx5qK7TLFHk1b2ZFRgVXF4laMNTBoMJC61MtALvHYnV02nDUTMq9ad3iEE/0KDgssKrlOHghnJPDR8ckkNkoxbi5rY5a1SO73ZElrnlJl0jPVJtPWDbHr0LU8wWJlCWgq80QS45Oy1wlcWtq6wudQub67fAbfPRnp0lnhq0cN2znBO0b1xBg32wWxnEOyd/CwzoOinBG5owNW25knszcnZ9bIB0NRl7DMhGbrpnNYClwZm34oF9BXjkm7u5oKVYVApk0+JIDMWvlg0MRuSawnQdgLXUGN08+xRi6Nx8o8TqU/VxQUlX9mNhJGQ6mVjQS7rVgNKx9N2NPhnp+NdENveRvhietYLWCXr+OpBBLqs7LphL7DkOeb6glYJV2ooh3fVPEtfFvFBraNWDFDP2OO6UNyT0jGfSrOxwUqtmKjiouwTcV38F02L2UBU/A9Fd/HwwLsJs8tw2AVP5B62rFdxQ/xI4HIdsuyWaK0cbfqZcJB/DiCh/nxkwiuU/FT/EzBz1X8Ar/k2K/kwK9V/AYb+fZb+fY7Er+XxB/CCv6oYjO2CMwplFoq/oQ/K/iLir/ikQgeCVfhbxFcoeLv+IeKf+JfEfybY/+J4L8qHsX/VG4foYiAKipwQBWVooqzbRHspjYRDFcLJSKqyS1qwooIRUStIlQp7BFV1IlZtC1/m4eDIhwREUXMVkVUnKWKOWLuGcvcPa2KeTjAtfOrxdmqWIAs5UmlY9DCVaI+AkPFM6AJtBQOUuc1tp42tWTyaO/Vpj7Unki4pX1ugaNIZpbi9FW9w7If2120hAdZaZOyr10WayqStjmd19IZW2rWeG1oKA/V5GErO4D+zh5qZW7oWoqH2GRf4shxR6UR/NgUEgvFObw7iEWym04k9HFKN57CbdpUqErJ3iqbcnwzvwgygVnDVrpTS4z2pofYfQ0V6318WU0HpR0NAheXW9JLI6vo7emUEhcLrC/dtBURIZmXsN+MzbxsqSyBw7I7vKxASItGT2D21JS7mnV6Vlq/Kmuk9R7L7Mkmk2yNC+XJ9CEJZBnvCtr4ePLok3NjTqNZkdLYE50/Q99awsSCWd7FG704t8Q1iMU75BjSnumSO7ixcBT6Lc7moK0mWmdsqsqWYiiAm9M+9Pris3QMw09vDxtpuaPnxApERGBH2S3GjDnWKLClaB6X190WSWXfHVLNeY6ax5E508/LhSV08MxN6wndOCLv27EuuenvEstnznG/51s4LSYdFnoAAAQAT+eE+xWIctgyTC5kF1bo1PSXyjtdwn0RWB0rtbJgXINJ3RyxR0NihYgxCYTzg/kMsgwpu2DZ8jlXUM20M9KvTYqIq6JZtKq4Ec9X8TzcoOI5eK6K62Wxf7Z8XM6SLFaz4rJNYNW7DE9XRZusmmtYDMVaFaPgZXLZFB62ZtaY7kWEbYQ2rEnM3P21qak3RbAdDnZb1liWW1ntMk3vAq7TYw/lOt6T5zYkuzRziHFvdBkLfMNSjFHeRp/YpKuyNG/T9NkO1uc+3nY2Od8o2AfdL0uqbcsFytNjGos748Zqx6TT7vj/+8JbXrxMFLRLERvYmJaFjZlfeh2WIoz1AKowWzbNpKKyEWYfeyHpADbxj62kM77Vm2d7zM+zuYYNLOc7+HYbKvgLrI+3nISIz0HgFCqPoyp+L4IDJ6EcR3W8+Thq4i3HEYrfw7cTqH0A6mnUBXDMUXUxnw1Q+FxFOCsILIZ6rMZitKERa7AOa9HJ2SWuIuxwYElKAhMOJaEFsJN0iJ+7+NnlyOYW8oDeQO6A/Fo5Hp1VGml0VhlQNyBIqBFCXUi/LaHnltNzG+g3CXWeq8qHutGDmg9QYI8P8B5U8he45F6EB+5GhAhCJzF775mvPfnA5PtpRNkNr5zAWcBpzMkdnMBcb2zKgnpiBxGF6bYGOm4V3dTBiB7iU6KPuzh89Jd46MPELHMiQK4L0U2qwrEo7FsksDeBHn70Yh+f0qwDnJTOWHA35nX7uJonMH8arllOgHdQ+05m2a4cTy7wsFTSmqc5WAT2+ypkLso0mOOLnyB/vvSQI2kPfdTtSFZdHk+yQJ8vr8OTN9eXF58ghMICeyliX47Aub7AftruCjzCvJZzu+JnBvM46qvuw8KBCg71DVTSQ32ncA4n6wdOYdGhCfqZShcHaM8SSS0N8MHu7q48DH3cnP05GHb5GA76RqXoPLm2zTWKbRczqLuA+mZPd2OR+FyCOgxwe16aE582T2EduQ45CSC4btJ80zN/W3nml2325TT7ihyzt/lmD/i6r6LZcm5jAU0TOM9XtHwmlfMg/xE1yMRKYBmG0AKd230wR/1GX/2l9Lq7qfd56hsKql9RxMUGtRzGIt54p6Q3eNLlhgtyxD3f2B14hrY6TueBTufWfNAXGHQGzRxBVT5MdhoezA18kx4NNOdzpnOiHPBPhACu9M+zSTBCXsoJX8ob8DZ9uDl+GrEAUy1cdQJN0s6KnAAeob1X5ygI+wrC9GyCIod82HJE98vTcJ7qEaoOPB7Vz6Tqa5+gaiFbME/dRZ46pTlILfEp97lqrqO7r89Ro/hqFEZ4yzQXjvkuvMwTPLuZ6TJpiVLAkhtYhW7MUTHbVzF7miVyJOlbknoAAANDylNuwvKS6UavFrUE70N8oKKZSdvSN1C10k3bZm+vnELLaayUwFadQGv+VrmJSXwzk/gW1shbyXNTTm1p8TG2YJx7VDhUmlTAoTKkKjhqI+shMohT1t265paV8yrnBid4AiE/WW93VGx1yludr6KOebbNma9jtrkq6nCNU74kdZTJUOlQ1+JZCEYCsg/31G7xolDTzG3FPGo7lqeyN8fzNb7KGvcUjIRkX++JWuNVleo4A1pA0p6cPVrt8IP8vBx4/Bd6UILN8QLcO3NwBJ0aCYeaxMGbhidnqycnFHfOvxNYky9re46skNcFSmpSFq8unqwer/uLxCerxlopcV2+xPacBi7iS4w4PYRwKCk7wPkXOJw3iVWORiFaxDqxksX3NXy/lXv2tVzzOrweb/Cot/jU233qnT71Lo96Pz7AZJHUh9hnHfdm78WHvdn78RGP+ig+5s3e789+Gp/BF4jzi977V/G1xwBQSwECFAMUAAAICABLeXpTClvdwFAAAABRAAAAFAAAAAAAAAAAAAAApIEAAAAATUVUQS1JTkYvTUFOSUZFU1QuTUZQSwECFAMKAAAIAABLeXpTAAAAAAAAAAAAAAAACQAAAAAAAAAAABAA7UGCAAAATUVUQS1JTkYvUEsBAhQDCgAACAAAS3l6UwAAAAAAAAAAAAAAAAMAAAAAAAAAAAAQAO1BqQAAAGlvL1BLAQIUAwoAAAgAAEt5elMAAAAAAAAAAAAAAAALAAAAAAAAAAAAEADtQcoAAABpby9ob3Rtb2thL1BLAQIUAwoAAAgAAEt5elMAAAAAAAAAAAAAAAAUAAAAAAAAAAAAEADtQfMAAABpby9ob3Rtb2thL2V4YW1wbGVzL1BLAQIUAwoAAAgAAEp5elMAAAAAAAAAAAAAAAAcAAAAAAAAAAAAEADtQSUBAABpby9ob3Rtb2thL2V4YW1wbGVzL2xhbWJkYXMvUEsBAhQDFAAACAgASnl6U0rhNErBCwAApCAAACkAAAAAAAAAAAAAAKSBXwEAAGlvL2hvdG1va2EvZXhhbXBsZXMvbGFtYmRhcy9MYW1iZGFzLmNsYXNzUEsFBgAAAAAHAAcAxgEAAGcNAAAAAA==", toBase64String(bytes));
    }

    @Test
    @DisplayName("writeFieldSignature(classType fieldSignature)")
    public void testFieldSignature() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new NodeMarshallingContext(baos)) {
            FieldSignature fieldSignature = FieldSignatures.of(StorageTypes.CONTRACT, "balance", StorageTypes.BIG_INTEGER);

            context.writeObject(FieldSignature.class, fieldSignature);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcM/xQAB2JhbGFuY2Ua", toBase64String(bytes));
    }

    @Test
    @DisplayName("writeStorageReference(storageReference)")
    public void testStorageReference() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new NodeMarshallingContext(baos)) {
            StorageReference storageReference = StorageValues.reference(
            		TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
            		new BigInteger("19992")
            );

            context.writeObject(StorageReference.class, storageReference);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcl///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggBOGA==", toBase64String(bytes));
    }

    @Test
    @DisplayName("testWriteTransactionReference(transactionReference)")
    public void testTransactionReference() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new NodeMarshallingContext(baos)) {
            context.writeObject(TransactionReference.class, TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"));
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXch/9DklkaMJfylkXmIX6fF/09EDvvQ4MlsJCa3mXM2YZiC", toBase64String(bytes));
    }

    @Test
    @DisplayName("writeFieldSignature(basicType fieldSignature)")
    public void testFieldSignatureBasicType() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new NodeMarshallingContext(baos)) {
            context.writeObject(FieldSignature.class, FieldSignatures.of(StorageTypes.STORAGE_TREE_INTMAP_NODE, "size", StorageTypes.INT));
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcJ/yYABHNpemUE", toBase64String(bytes));
    }

    @Test
    @DisplayName("StorageValues.stringOf(\"hello\")")
    public void testStringValue() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new NodeMarshallingContext(baos)) {
            StorageValues.stringOf("hello").into(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcICgAFaGVsbG8=", toBase64String(bytes));
    }

    @Test
    @DisplayName("StorageValues.stringOf(very long string)")
    public void testVeryLongStringValue() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new NodeMarshallingContext(baos)) {
            StorageValue storageValue = StorageValues.stringOf("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");
            storageValue.into(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXoAAAQAChTMck8wQUJYb0FBQVFBQXdBSlkyaGhhVzUwWlhOMC8vL1E1SlpHakNYOHBaRjVpRitueGY5UFJBNzcwT0RKYkNRbXQ1bHpObUdZZ2dRQUU0Z0FENkFBQmY4QUFBOURVRXNEQkJRQUFBZ0lBQzkyZUZNS1c5M0FVQUFBQUZFQUFBQVVBQUFBVFVWVVFTMUpUa1l2VFVGT1NVWkZVMVF1VFVielRjekxURXN0THRFTlN5MHF6c3pQczFJdzFEUGc1WEl1U2swc1NVM1JkYXEwVXZCTkxFdk5VL0JLTEZJSXlDbE56OHhUTU5ZekFxbHhLczNNU2RIMVNzbldEUzVJVFFacU5PVGw0dVVDQUZCTEF3UUtBQUFJQUFBdmRuaFRBQUFBQUFBQUFBQUFBQUFBQ1FBQUFFMUZWRUV0U1U1R0wxQkxBd1FLQUFBSUFBQXZkbmhUQUFBQUFBQUFBQUFBQUFBQUF3QUFBR2x2TDFCTEF3UUtBQUFJQUFBdmRuaFRBQUFBQUFBQUFBQUFBQUFBQ3dBQUFHbHZMMmh2ZEcxdmEyRXZVRXNEQkFvQUFBZ0FBQzkyZUZNQUFBQUFBQUFBQUFBQUFBQVVBQUFBYVc4dmFHOTBiVzlyWVM5bGVHRnRjR3hsY3k5UVN3TUVDZ0FBQ0FBQUxuWjRVd0FBQUFBQUFBQUFBQUFBQUJ3QUFBQnBieTlvYjNSdGIydGhMMlY0WVcxd2JHVnpMMnhoYldKa1lYTXZVRXNEQkJRQUFBZ0lBQzUyZUZOSzRUUkt3UXNBQUtRZ0FBQXBBQUFBYVc4dmFHOTBiVzlyWVM5bGVHRnRjR3hsY3k5c1lXMWlaR0Z6TDB4aGJXSmtZWE11WTJ4aGMzTzFXUWQ4RzlVWi96L1oxdG55T1ZKRUJuR0lNekNKTER0eG5BbGtFTWM0aVJQSFRtTW53VkNnWi9sc1h5emRHZWtVU0dtaExiU3NsdTZXN2wwNjZBZ2x5eFJLZDB2cDNvdnV2UWZkcGZ6ZjNmbXN5SklzUnUyZlQ5Kzk5Nzd2KzMvanZlOTc4b09QVHR3UFlLUFlHTUo2M0Z3RjUyZEZKSVNGdUtVR1lkeXE0TFlRZ25paGZIbVJmTnd1SHk4TzhmRVNCUyt0eGN2dzhtcThRc0VyYThqektqbithamw0UjVTQzNsaUxOK0hOVVFpOFZjNitMWW9BM2hGRkJlNk1vbEtPaGZGdUJlK1JxOTRid2wxNFh4UlZPQ1pmNzQ1UzZRbEpuYXpGS1p5V0lpZWlVSEJmRk5WNElJb2ErUWpoemxwOEhKK1FXaitwNEZOUjFPS0JFRDZMQnlYbjV4UThGTUlxZkQ0S0ZWOEs4ZkZsdXoAAAQAZkFyVWRUaG1LUytydUFiQXRHOVhUMWRldy9zdmJLcjUyQm5YLy9lenA1K2dYbmRoN1VqV210S3MwZGJ0eHNqWGFhdGoranBUUUkxaG5sRXo5aFdPaU53YnJkaHRkcmFtSmJpWDJ2Q0d0SmJzN2FSYk8zanREYWlkeHNaVzNMMEdTT21abWZUdWtEM3pCeWJweTlKYXVaSTZ6N3RxRGFZMURzczAwNXJDWHZUVm9xdXN1eFJQUzF3bnVRWnRleVVSUmI5R2kwMW50UXpaRXNORG1tWjFtNzNrK3VEV3NyS21qWXhqV2NIazBaaWozNlU5cnVtT2tyNjdMUmhqc2lWbXczVHNMY0tOTWNLZTJJNlU5TkJnY29PNGhVSWR4dW0zcE5ORGVycGZnbGFLckVTV3ZLZ2xqYmt1emRZYVk4YWRPT0MvY1JrcFBTRFJzYmdlTHRwV3JabUc1Ykp1Y1dsdlVHa2pYb0FBQVFBUlZic1NGc3AzMWxVVXNRT2lUcm94cFNFNDg4MWRPd1JMWmtsd2hWbEJrTmdyazBKcnFjUEdmYW9sYlg3SGVNcVlsSkRoV1ZTV2tQQk5UdDF1MC9hbTZDWHpsemdTcWlYZzN0MXJoL2Fydy9yYWQxTTZQMVdKelVmZGNSM0NTd3Z2cVIzdUZmYTFKSFVNcFJWUFptOUpYbjZ0SlJ2R2tNZ0YvSTFZNmV6Q2JKT0J4R2JZY1UrYTN5L25za21aZUtONkhhN2w0Um54NXFLN1RMRkhrMWIyWkZSZ1ZYRjRsYU1OVEJvTUpDNjFNdEFMdkhZblYwMm5EVVRNcTlhZDNpRUUvMEtEZ3NzS3JsT0hnaG5KUERSOGNra05rb3hiaTVyWTVhMVNPNzNaRWxybmxKbDBqUFZKdFBXRGJIcjBMVTh3V0psQ1dncTgwUVM0NU95MXdsY1d0cTZ3dWRRdWI2N2ZBYmZQUm5wMGxuaHEwY04yem5CTzBiMXhCZzMyd1d4bkVPeWQvQ3d6b09pbkJHNW93TlcyNWtuc3pjblo5YklCME5SbDdETWhHYnJwbk5ZQ2x3Wm0zNG9GOUJYamttN3U1b0tWWVZBcGswK0pJRE1XdmxnME1SdVNhd25RZGdMWFVHTjA4K3hSaTZOeDhvOFRxVS9WeFFVbFg5bU5oSkdRNm1WalFTN3JWZ05LeDlOMk5QaG5wK05kRU52ZVJ2aGlldFlMV0NYcitPcEJCTHFzN0xwaEw3RGtPZWI2Z2xZSlYyb29oM2ZWUEV0ZkZ2RkJyYU5XREZEUDJPTzZVTnlUMGpHZlNyT3h3VXF0bUtqaW91d1RjVjM4RjAyTDJVQlUvQTlGZC9Id3dMc0pzOHR3MkFWUHoAAAQANUI2MnJGZHhRL3hJNEhJZHN1eVdhSzBjYmZxWmNKQi9EaUNoL254a3dpdVUvRlQvRXpCejFYOEFyL2sySy9rd0s5Vi9BWWIrZlpiK2ZZN0VyK1h4Qi9DQ3Y2b1lqTzJDTXdwbEZvcS9vUS9LL2lMaXIvaWtRZ2VDVmZoYnhGY29lTHYrSWVLZitKZkVmeWJZLytKNEw4cUhzWC9WRzRmb1lpQUtpcHdRQldWb29xemJSSHNwallSREZjTEpTS3F5UzFxd29vSVJVU3RJbFFwN0JGVjFJbFp0QzEvbTRlRElod1JFVVhNVmtWVW5LV0tPV0x1R2N2Y1BhMktlVGpBdGZPcnhkbXFXSUFzNVVtbFk5RENWYUkrQWtQRk02QUp0QlFPVXVjMXRwNDJ0V1R5YU8vVnBqN1Vua2k0cFgxdWdhTklacGJpOUZXOXc3SWYyMTIwaEFkWmFaT3lyMTBXYXlxU3RqbWQxOUlaVzJyV2VHMW9LQS9WNUdFck80RCt6aDVxWlc3b1dvcUgyR1JmNHNoeFI2VVIvTmdVRWd2Rk9idzdpRVd5bTA0azlIRktONTdDYmRwVXFFckozaXFiY253enZ3Z3lnVm5EVnJwVFM0ejJwb2ZZZlEwVjYzMThXVTBIcFIwTkFoZVhXOUpMSTZ2bzdlbVVFaGNMckMvZHRCVVJJWm1Yc04rTXpieHNxU3lCdzdJN3ZLeEFTSXRHVDJEMjFKUzdtblY2VmxxL0ttdWs5UjdMN01rbWsyeU5DK1hKOUNFSlpCbnZDdHI0ZVBMb2szTmpUcU5aa2RMWUU1MC9ROTlhd3NTQ1dkN0ZHNzA0dDhRMWlNVTc1QmpTbnVtU083aXhjQlQ2TGM3bW9LMG1XbWRzcXNxV1lpaUFtOU0rOVByaXMzUU13MDl2RHh0cHVhUG54QXBFUkdCSDJYb0FBQVFBTGNhTU9kWW9zS1ZvSHBmWDNSWkpaZDhkVXMxNWpwckhrVG5Uejh1RkpYVHd6RTNyQ2QwNEl1L2JzUzY1NmU4U3kyZk9jYi9uV3pndEpoMFdUK2VFK3hXSWN0Z3lUQzVrRjFibzFQU1h5anRkd24wUldCMHJ0YkpnWElOSjNSeXhSME5paFlneENZVHpnL2tNc2d3cHUyRFo4amxYVU0yME05S3ZUWXFJcTZKWnRLcTRFYzlYOFR6Y29PSTVlSzZLNjJXeGY3WjhYTTZTTEZhejRySk5ZTlc3REU5WFJadXNtbXRZRE1WYUZhUGdaWExaRkI2Mlp0YVk3a1dFYllRMnJFbk0zUDIxcWFrM1JiQWREblpiMWxpV1cxbnRNazN2QXE3VFl3L2xPdDZUNXpZa3V6UnppSEZ2ZHoAAAQAQmtMZk1OU2pGSGVScC9ZcEt1eU5HL1Q5TmtPMXVjKzNuWTJPZDhvMkFmZEwwdXFiY3NGeXROakdvczc0OFpxeDZUVDd2ai8rOEpiWHJ4TUZMUkxFUnZZbUphRmpabGZlaDJXSW96MUFLb3dXemJOcEtLeUVXWWZleUhwQURieGo2MmtNNzdWbTJkN3pNK3p1WVlOTE9jNytIWWJLdmdMckkrM25JU0l6MEhnRkNxUG95cCtMNElESjZFY1IzVzgrVGhxNGkzSEVZcmZ3N2NUcUgwQTZtblVCWERNVVhVeG53MVErRnhGT0NzSUxJWjZyTVppdEtFUmE3QU9hOUhKMlNXdUl1eHdZRWxLQWhNT0phRUZzSk4waUorNytObmx5T1lXOG9EZVFPNkEvRm81SHAxVkdtbDBWaGxRTnlCSXFCRkNYVWkvTGFIbmx0TnpHK2czQ1hXZXE4cUh1dEdEbWc5UVlJOFA4QjVVOGhlNDVGNkVCKzVHaEFoQ0p6Rjc3NW12UGZuQTVQdHBSTmtOcjV6QVdjQnB6TWtkbk1CY2IyektnbnBpQnhHRjZiWUdPbTRWM2RUQmlCN2lVNktQdXpoODlKZDQ2TVBFTEhNaVFLNEwwVTJxd3JFbzdGc2tzRGVCSG43MFloK2YwcXdEbkpUT1dIQTM1blg3dUpvbk1IOGFybGxPZ0hkUSswNW0yYTRjVHk3d3NGVFNtcWM1V0FUMit5cGtMc28wbU9PTG55Qi92dlNRSTJrUGZkVHRTRlpkSGsreVFKOHZyOE9UTjllWEY1OGdoTUlDZXlsaVg0N0F1YjdBZnRydUNqekN2Slp6dStKbkJ2TTQ2cXZ1dzhLQkNnNzFEVlRTUTMybmNBNG42d2RPWWRHaENmcVpTaGNIYU04U1NTME44TUh1N3E0OERIM2NuUDA1R0hiNUdBNzZScVhvUExtMnpUV0tiUmN6cUx1QSttWlBkMk9SK0Z5Q09neHdlMTZhRTU4MlQyRWR1UTQ1Q1NDNGJ0Sjgwek4vVzNubWwyMzI1VFQ3aWh5enQvbG1EL2k2cjZMWmNtNWpBVTBUT005WHRId21sZk1nL3hFMXlNUktZQm1HMEFLZDIzMHdSLzFHWC8ybDlMcTdxZmQ1NmhzS3FsOVJ4TVVHdFJ6R0l0NTRwNlEzZU5MbGhndHl4RDNmMkIxNGhyWTZUdWVCVHVmV2ZOQVhHSFFHelJ4QlZUNU1kaG9lekExOGt4NE5OT2R6cG5PaUhQQlBoQUN1OU0relNUQkNYc29KWDhvYjhEWjl1RGwrR3JFQVV5MWNkUUpOMHM2S25BQWVvYjFYNXlnSSt3ckM5R3lDSW9kODJISkU5OHZUY3oAAAQASjdxRWFvT1BCN1Z6NlRxYTUrZ2FpRmJNRS9kUlhvQUFBT0FuanFsT1VndDhTbjN1V3F1bzd1dnoxR2orR29VUm5qTE5CZU8rUzY4ekJNOHU1bnBNbW1KVXNDU0cxaUZic3hSTWR0WE1YdWFKWElrNlZ1U3lsTnV3dktTNlVhdkZyVUU3ME44b0tLWlNkdlNOMUMxMGszYlptK3ZuRUxMYWF5VXdGYWRRR3YrVnJtSlNYd3prL2dXMXNoYnlYTlRUbTFwOFRHMllKeDdWRGhVbWxUQW9US2tLamhxSStzaE1vaFQxdDI2NXBhVjh5cm5CaWQ0QWlFL1dXOTNWR3gxeWx1ZHI2S09lYmJObWE5anRya3E2bkNOVTc0a2RaVEpVT2xRMStKWkNFWUNzZy8zMUc3eG9sRFR6RzNGUEdvN2xxZXlOOGZ6TmI3S0d2Y1VqSVJrWCsrSld1TlZsZW80QTFwQTBwNmNQVnJ0OElQOHZCeDQvQmQ2VUlMTjhRTGNPM053QkowYUNZZWF4TUdiaGlkbnF5Y25GSGZPdnhOWWt5OXJlNDZza05jRlNtcFNGcTh1bnF3ZXIvdUx4Q2VyeGxvcGNWMit4UGFjQmk3aVM0dzRQWVJ3S0NrN3dQa1hPSnczaVZXT1JpRmF4RHF4a3NYM05YeS9sWHYydFZ6ek9yd2ViL0NvdC9qVTIzM3FuVDcxTG85NlB6N0FaSkhVaDlobkhmZG03OFdIdmRuNzhSR1AraWcrNXMzZTc4OStHcC9CRjRqemk5NzdWL0cxeHdCUVN3RUNGQU1VQUFBSUNBQXZkbmhUQ2x2ZHdGQUFBQUJSQUFBQUZBQUFBQUFBQUFBQUFBQUFwSUVBQUFBQVRVVlVRUzFKVGtZdlRVRk9TVVpGVTFRdVRVWlFTd0VDRkFNS0FBQUlBQUF2ZG5oVEFBQUFBQUFBQUFBQUFBQUFDUUFBQUFBQUFBQUFBQkFBN1VHQ0FBQUFUVVZVUVMxSlRrWXZVRXNCQWhRRENnQUFDQUFBTDNaNFV3QUFBQUFBQUFBQUFBQUFBQU1BQUFBQUFBQUFBQUFRQU8xQnFRQUFBR2x2TDFCTEFRSVVBd29BQUFnQUFDOTJlRk1BQUFBQUFBQUFBQUFBQUFBTEFBQUFBQUFBQUFBQUVBRHRRY29BQUFCcGJ5OW9iM1J0YjJ0aEwxQkxBUUlVQXdvQUFBZ0FBQzkyZUZNQUFBQUFBQUFBQUFBQUFBQVVBQUFBQUFBQUFBQUFFQUR0UWZNQUFBQnBieTlvYjNSdGIydGhMMlY0WVcxd2JHVnpMMUJMQVFJVUF3b0FBQWdBQUM1MmVGTUFBQUFBQUFBQUFBQUFBQUFjQXfPQUFBQUFBQUFBQUFFQUR0UVNVQkFBQnBieTlvYjNSdGIydGhMMlY0WVcxd2JHVnpMMnhoYldKa1lYTXZVRXNCQWhRREZBQUFDQWdBTG5aNFUwcmhORXJCQ3dBQXBDQUFBQ2tBQUFBQUFBQUFBQUFBQUtTQlh3RUFBR2x2TDJodmRHMXZhMkV2WlhoaGJYQnNaWE12YkdGdFltUmhjeTlNWVcxaVpHRnpMbU5zWVhOelVFc0ZCZ0FBQUFBSEFBY0F4Z0VBQUdjTkFBQUFBQUE9", toBase64String(bytes));
    }

    @Test
    @DisplayName("StorageValues.intOf(1993)")
    public void testIntValue() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new NodeMarshallingContext(baos)) {
            StorageValues.intOf(1993).into(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcFDgAAB8k=", toBase64String(bytes));
    }

    @Test
    @DisplayName("StorageValues.TRUE")
    public void testBooleanValue() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new NodeMarshallingContext(baos)) {
            StorageValues.TRUE.into(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcBAA==", toBase64String(bytes));
    }

    @Test
    @DisplayName("StorageValues.byteOf(32)")
    public void testByteValue() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new NodeMarshallingContext(baos)) {
        	StorageValues.byteOf((byte) 32).into(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcCAiA=", toBase64String(bytes));
    }

    @Test
    @DisplayName("StorageValues.charOf('D')")
    public void testCharValue() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new NodeMarshallingContext(baos)) {
            StorageValues.charOf('D').into(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcDAwBE", toBase64String(bytes));
    }

    @Test
    @DisplayName("StorageValues.shortOf(44)")
    public void testShortValue() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new NodeMarshallingContext(baos)) {
            StorageValues.shortOf((short) 44).into(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcDCQAs", toBase64String(bytes));
    }

    @Test
    @DisplayName("StorageValues.longOf(1238769181L)")
    public void testLongValue() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new NodeMarshallingContext(baos)) {
            StorageValues.longOf(1238769181L).into(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcJBwAAAABJ1h4d", toBase64String(bytes));
    }

    @Test
    @DisplayName("StorageValues.doubleOf(1238769181.9)")
    public void testDoubleValue() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new NodeMarshallingContext(baos)) {
            StorageValues.doubleOf(1238769181.9).into(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcJBEHSdYeHeZma", toBase64String(bytes));
    }

    @Test
    @DisplayName("StorageValues.floatOf(23.7f)")
    public void testFloatValue() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new NodeMarshallingContext(baos)) {
            StorageValues.floatOf(23.7f).into(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcFBUG9mZo=", toBase64String(bytes));
    }

    @Test
    @DisplayName("new ConstructorCallTransactionRequest(..) manifest")
    public void testConstructorCallTransactionRequest() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new NodeMarshallingContext(baos)) {
            var constructorSignature = ConstructorSignatures.of(StorageTypes.MANIFEST, StorageTypes.BIG_INTEGER);

            var constructorCall = TransactionRequests.constructorCall(
                   "".getBytes(),
                   StorageValues.reference(TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                   BigInteger.ONE,
                   "chaintest",
                   BigInteger.valueOf(11500),
                   BigInteger.valueOf(500),
                   TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
                   constructorSignature,
                   StorageValues.bigIntegerOf(999)
            );

            constructorCall.intoWithoutSignature(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXc/BAAJY2hhaW50ZXN0///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggQALOwAAfQABQEGAAPnEwEa", toBase64String(bytes));
    }

    @Test
    @DisplayName("ConstructorSignatures.of(..) manifest")
    public void testConstructorSignature() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new NodeMarshallingContext(baos)) {
        	ConstructorSignatures.of(StorageTypes.MANIFEST, StorageTypes.BIG_INTEGER).into(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXcDEwEa", toBase64String(bytes));
    }

    @Test
    @DisplayName("new StaticMethodCallTransactionRequest(..) balance of gasStation")
    public void testStaticMethodCallTransactionRequest() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new NodeMarshallingContext(baos)) {
            var nonVoidMethodSignature = MethodSignatures.of(
            		StorageTypes.GAS_STATION,
                    "balance",
                    StorageTypes.BIG_INTEGER,
                    StorageTypes.STORAGE
            );

            var staticMethodCallTransactionRequest = TransactionRequests.staticMethodCall(
                    "".getBytes(),
                    StorageValues.reference(TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                    BigInteger.ONE,
                    "chaintest",
                    BigInteger.valueOf(5000),
                    BigInteger.valueOf(4000),
                    TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
                    nonVoidMethodSignature,
                    StorageValues.reference(TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO)
            );

            staticMethodCallTransactionRequest.intoWithoutSignature(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXdHBgAJY2hhaW50ZXN0///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggQAE4gAD6AABQELACgAB2JhbGFuY2UDFxo=", toBase64String(bytes));
    }

    @Test
    @DisplayName("new StaticMethodCallTransactionRequest(..) receive")
    public void testVoidStaticMethodCallTransactionRequest() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new NodeMarshallingContext(baos)) {
            var staticMethodCallTransactionRequest = TransactionRequests.staticMethodCall(
                    "".getBytes(),
                    StorageValues.reference(TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                    BigInteger.ONE,
                    "chaintest",
                    BigInteger.valueOf(5000),
                    BigInteger.valueOf(4000),
                    TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
                    MethodSignatures.RECEIVE_INT,
                    StorageValues.intOf(300)
                );

            staticMethodCallTransactionRequest.intoWithoutSignature(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXdJBgAJY2hhaW50ZXN0///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggQAE4gAD6AABQEOAAABLBsAB3JlY2VpdmUCBA==", toBase64String(bytes));
    }

    @Test
    @DisplayName("new StaticMethodCallTransactionRequest(..) nonce")
    public void testNonVoidStaticMethodCallTransactionRequest() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new NodeMarshallingContext(baos)) {
            var staticMethodCallTransactionRequest = TransactionRequests.staticMethodCall(
                    "".getBytes(),
                    StorageValues.reference(TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                    BigInteger.ONE,
                    "chaintest",
                    BigInteger.valueOf(5000),
                    BigInteger.valueOf(4000),
                    TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
                    MethodSignatures.NONCE
            );

            staticMethodCallTransactionRequest.intoWithoutSignature(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXdCBgAJY2hhaW50ZXN0///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggQAE4gAD6AABQASAAVub25jZQEa", toBase64String(bytes));
    }

    @Test
    @DisplayName("TransactionRequests.instanceMethodCall(..) receive")
    public void testVoidInstanceMethodCallTransactionRequest() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new NodeMarshallingContext(baos)) {
            var request = TransactionRequests.instanceMethodCall(
                    "".getBytes(),
                    StorageValues.reference(TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                    BigInteger.ONE,
                    "chaintest",
                    BigInteger.valueOf(5000),
                    BigInteger.valueOf(4000),
                    TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
                    MethodSignatures.RECEIVE_INT,
                    StorageValues.reference(TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                    StorageValues.intOf(300)
            );

            request.intoWithoutSignature(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXc8BwAJY2hhaW50ZXN0///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggQAE4gAD6AABQAAAAEs", toBase64String(bytes));
    }

    @Test
    @DisplayName("TransactionRequests.instanceMethodCall(..) getGamete")
    public void testNonVoidInstanceMethodCallTransactionRequest() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new NodeMarshallingContext(baos)) {
            var request = TransactionRequests.instanceMethodCall(
                    "".getBytes(),
                    StorageValues.reference(TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                    BigInteger.ONE,
                    "chaintest",
                    BigInteger.valueOf(5000),
                    BigInteger.valueOf(4000),
                    TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
                    MethodSignatures.GET_GAMETE,
                    StorageValues.reference(TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO)
            );

            request.intoWithoutSignature(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("rO0ABXdQBQAJY2hhaW50ZXN0///Q5JZGjCX8pZF5iF+nxf9PRA770ODJbCQmt5lzNmGYggQAE4gAD6AABQATAAlnZXRHYW1ldGUBCv8ABkdhbWV0ZQA=", toBase64String(bytes));
    }

    @Test
    @DisplayName("new JarStoreTransactionRequest(..)")
    public void testJarStoreTransactionRequest() throws IOException {
        byte[] bytes;

        try (var baos = new ByteArrayOutputStream(); var context = new NodeMarshallingContext(baos)) {
            var jarStoreTransactionRequest = TransactionRequests.jarStore(
                    "".getBytes(),
                    StorageValues.reference(TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"), BigInteger.ZERO),
                    BigInteger.ONE,
                    "chaintest",
                    BigInteger.valueOf(5000),
                    BigInteger.valueOf(4000),
                    TransactionReferences.of("d0e496468c25fca59179885fa7c5ff4f440efbd0e0c96c2426b7997336619882"),
                    bytesOf("lambdas.jar")
            );

            jarStoreTransactionRequest.intoWithoutSignature(context);
            context.flush();
            bytes = baos.toByteArray();
        }

        Assertions.assertEquals("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",
        		toBase64String(bytes));
    }

    private static byte[] bytesOf(String fileName) throws IOException {
        return Files.readAllBytes(Paths.get("./jars/" + fileName));
    }
}