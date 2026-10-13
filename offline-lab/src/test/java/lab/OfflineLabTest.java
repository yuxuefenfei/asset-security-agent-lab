package lab;

import org.junit.jupiter.api.Test;

/** 将原有可独立运行的自检接入 Maven 的 test 生命周期。 */
class OfflineLabTest {
    @Test
    void rulesAndToolConstraints() throws Exception {
        SelfTest.main(new String[0]);
    }

    @Test
    void modelToolCallingProtocol() throws Exception {
        FakeModelTest.main(new String[0]);
    }

    @Test
    void httpApiContracts() throws Exception {
        HttpApiTest.main(new String[0]);
    }
}
