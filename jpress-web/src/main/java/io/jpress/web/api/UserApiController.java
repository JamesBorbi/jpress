/**
 * Copyright (c) 2016-2020, Michael Yang 杨福海 (fuhai999@gmail.com).
 * <p>
 * Licensed under the GNU Lesser General Public License (LGPL) ,Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/lgpl-3.0.txt
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.jpress.web.api;

import com.jfinal.aop.Inject;
import com.jfinal.kit.HashKit;
import com.jfinal.kit.Ret;
import io.jboot.apidoc.annotation.Api;
import io.jboot.apidoc.annotation.ApiOper;
import io.jboot.apidoc.annotation.ApiPara;
import io.jboot.apidoc.annotation.ApiResp;
import io.jboot.utils.DateUtil;
import io.jboot.web.controller.annotation.GetRequest;
import io.jboot.web.controller.annotation.PostRequest;
import io.jboot.web.controller.annotation.RequestMapping;
import io.jboot.web.json.JsonBody;
import io.jpress.JPressConsts;
import io.jpress.commons.utils.SessionUtils;
import io.jpress.model.Member;
import io.jpress.model.MemberGroup;
import io.jpress.model.MemberJoinedRecord;
import io.jpress.model.User;
import io.jpress.service.MemberGroupService;
import io.jpress.service.MemberService;
import io.jpress.service.UserService;
import io.jpress.web.base.ApiControllerBase;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * 用户相关的API
 */
@RequestMapping("/api/user")
@Api("用户相关API")
public class UserApiController extends ApiControllerBase {

    @Inject
    private UserService userService;
    @Inject
    private MemberService memberService;
    @Inject
    private MemberGroupService memberGroupService;


    @PostRequest
    @ApiOper("用户登录")
    @ApiResp(field = "Jwt", notes = "Jwt 的 token 信息", mock = "ey1NiJ9.eyJpYX0ifQ.Y3p4akomy4")
    public Ret login(@ApiPara(value = "登录账户", notes = "可以是邮箱") @NotNull String loginAccount
            , @ApiPara("登录密码") @NotNull String password) {
        User loginUser = userService.findByUsernameOrEmail(loginAccount);
        if (loginUser == null) {
            return Ret.fail("message", "没有该用户信息");
        }

        Ret ret = userService.doValidateUserPwd(loginUser, password);
        if (!ret.isOk()) {
            return ret;
        }

        SessionUtils.isLoginedOk(ret.get("user_id"));
        setJwtAttr(JPressConsts.JWT_USERID, ret.get("user_id"));

        String jwt = createJwtToken();
        return Ret.ok().set("Jwt", jwt);
    }


    @ApiOper("用户详情")
    @ApiResp(field = "user", dataType = User.class, notes = "用户信息")
    public Ret detail(@ApiPara("用户ID") @NotNull Long id) {
        User user = userService.findById(id);
        return Ret.ok().set("user", user.copy().keepSafe());
    }


    @ApiOper("更新用户信息")
    public Ret update(@ApiPara("用户 json 信息") @JsonBody @NotNull User user) {
        user.keepUpdateSafe();
        userService.update(user);
        return Ret.ok();
    }

    @PostRequest
    @ApiOper("更新用户密码")
    public Ret updatePassword(@ApiPara("用户ID") @NotNull Long userId
            , @ApiPara("用户新密码") @NotEmpty String newPassword
            , @ApiPara(value = "用户旧密码", notes = "如果登录用户是超级管理员，则可以不输入密码") String oldPassowrd) {

        User user = userService.findById(userId);
        if (user == null) {
            return Ret.fail("message", "该用户不存在");
        }

        String salt = user.getSalt();
        String hashedPass = HashKit.sha256(salt + newPassword);

        user.setPassword(hashedPass);
        userService.update(user);

        //移除用户登录 session
        SessionUtils.forget(userId);

        return Ret.ok();
    }


    @ApiOper("创建新的用户")
    @ApiResp(field = "userId", notes = "用户ID，用户创建成功后返回此数据", dataType = Long.class)
    public Ret create(@ApiPara("用户 json 信息") @JsonBody @NotNull User user) {
        userService.save(user);
        return Ret.ok().set("userId", user.getId());
    }
    @ApiOper("api加入会员")
//    @ApiResp(field = "userId", notes = "用户ID，用户创建成功后返回此数据", dataType = Long.class)
    @GetRequest
    public void addMember(@ApiPara("用户 json 信息") String jpressAppId) {

        //jpressAppId=asiamales_"+userId+"_2
        String[] strs = jpressAppId.split("_");
        Long userId = Long.valueOf(strs[1]);
        Long groupId = Long.valueOf(strs[2]);

        Member member = new Member();
        member.setUserId(userId);
        member.setGroupId(groupId);
        member.setCreated(new Date());
        member.setDuetime(DateUtil.parseDate("2050-12-31 23:59:00",DateUtil.datetimePattern));
        member.setSource("buy");
        member.setStatus(9);


        Member existModel = memberService.findByGroupIdAndUserId(groupId, userId);
        if (existModel != null && !existModel.getId().equals(member.getId())) {
            renderFailJson("用户已经加入该会员");
            return;
        }

        MemberGroup group = memberGroupService.findById(groupId);
        if (group == null || !group.isNormal()) {
            renderFailJson("该会员组不存在或已经被禁用。");
            return;
        }

        if (member.getId() == null) {
            MemberJoinedRecord joinedRecord = new MemberJoinedRecord();
            joinedRecord.setUserId(member.getUserId());
            joinedRecord.setGroupId(member.getGroupId());
            joinedRecord.setGroupName(group.getName());
            joinedRecord.setJoinCount(1);
            joinedRecord.setJoinType(member.getSource());
            joinedRecord.setCreated(new Date());
            joinedRecord.setJoinFrom(MemberJoinedRecord.JOIN_FROM_ADMIN);

            if (Member.SOURCE_BUY.equals(member.getSource())) {
                joinedRecord.setJoinPrice(group.getPrice());
            }

            if (memberService.saveOrUpdate(member) == null) {
                renderFailJson();
                return;
            }
        }

        memberService.saveOrUpdate(member);
        renderOkJson();

//        return Ret.ok().set("userId", userId);
    }
}
