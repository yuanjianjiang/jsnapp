<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:template match="/renew">
    <html>
    <head>
      <link rel="stylesheet" type="text/css" href="/style.css"/>
      <title>Password Renewal</title>
    </head>
    <body>
      <center>
      <xsl:if test="forced">
        <p><span style="color:#ff0000">Your password has expired!</span></p>
      </xsl:if>
      <p>Renew Your Password</p>
      <xsl:if test="error!=''">
        <p><span style="color:#ff0000"><xsl:value-of select="error"/></span></p>
      </xsl:if>
      <form method="post" action="/renew.do">
      <table>
        <xsl:apply-templates select="forced"/>
        <xsl:apply-templates select="requested"/>
        <tr>
        <td><i>New:</i></td><td colspan="2"><input class="text" type="password" name="password1" value="{password1}"/></td>
        </tr>
        <tr>
        <td><i>Repeat:</i></td><td colspan="2"><input class="text" type="password" name="password2" value="{password2}"/></td>
        </tr>
        <tr>
        <td colspan="3" align="center"><input class="button" type="submit" value="Renew"/></td>
        </tr>
      </table>
      </form>
      </center>
    </body>
    </html>
  </xsl:template>
  <xsl:template match="/renew/forced">
    <input type="hidden" name="reason" value="forced"/>
    <tr>
    <td><i>Username:</i></td><td><input class="text" type="text" readonly="true" name="username" value="{/renew/username}"/></td><td><i>(read-only)</i></td>
    </tr>
    <tr>
    <td><i>Current:</i></td><td><input class="text" type="password" readonly="true" name="password" value="{/renew/password}"/></td><td><i>(verified)</i></td>
    </tr>
  </xsl:template>
  <xsl:template match="/renew/requested">
    <input type="hidden" name="reason" value="requested"/>
    <tr>
    <td><i>Username:</i></td><td colspan="2"><input class="text" type="text" name="username" value="{/renew/username}"/></td>
    </tr>
    <tr>
    <td><i>Current:</i></td><td colspan="2"><input class="text" type="password" name="password" value=""/></td>
    </tr>
  </xsl:template>
</xsl:stylesheet>
