<#include "BaseEmail.ftl">

<#--
    This is a template for all emails related to the OTP Admin Dashboard. Any templates that use
    this template must implement a macro called `email_main`. The whole HTML email can then be
    rendered by the statement <@html_email/>.
-->

<#macro email_body>
<#-- header -->
<div>
    <h2>An email from the ${OTP_ADMIN_DASHBOARD_NAME}</h2>
</div>
<#-- main -->
<div>
    <@email_main/>
</div>
<#-- footer -->
<div>
    <p>
        <small>
            Manage subscriptions <a href="${OTP_ADMIN_DASHBOARD_URL}/account">here</a>.
        </small>
    </p>
</div>
</#macro>

