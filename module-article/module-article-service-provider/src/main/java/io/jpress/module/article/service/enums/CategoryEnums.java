package io.jpress.module.article.service.enums;

/**
 * desc
 * @param null
 * @return
 * @author zbd
 * @date 2021/4/22 17:09
 */
public enum CategoryEnums {
    /**
     * 操作  1.加工厂确认付款
     */
    NI(9,"亚洲帅哥"),
    TE(10,"蓝男色"),
    EL(11,"Virile"),
    TR(12,"WHOSEMAN"),
    FG(13,"蓝摄"),
    TF(14,"BLUE MEN"),
    TG(15,"型男帮"),
    TS(16,"男摄"),
    WD(17,"StyleMenX"),
    W(18,"漫摄"),
    P(19,"幻摄"),
    K(20,"泰国帅哥"),
    D(21,"名摄作品"),
    G(22,"ADONISJING 劉京"),
    A(23,"R2任壬"),
    N(24,"謝梓秋"),
    M(25,"JQVISION"),
    I(26,"BURNING ART"),
    O(27,"魔男志"),
    Q(28,"会员vip"),
    ;

    private CategoryEnums(Integer value, String title){
        this.value = value;
        this.title = title;
    }

    private Integer value;
    private String title;

    public Integer getValue() {
        return value;
    }
    public String getTitle() {
        return title;
    }
}