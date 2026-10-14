package testcases.ecoCardPortal;

import actions.common.BaseTest;
import org.testng.annotations.Test;
import pageObject.LoginOb;
import pageObject.PageGeneratorManager;

public class LoginTest extends BaseTest {
    LoginOb loginOb = PageGeneratorManager.getPage(LoginOb.class);


    @Test(description = "Đăng nhập thành công")
    public void loginSuccessWithValidUserNameAndPassword() {
        loginOb.openToEcoCardUrl();
    }
}
