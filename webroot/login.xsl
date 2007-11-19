<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:template match="/login">
    <html>
    <head>
      <link rel="stylesheet" type="text/css" href="/style.css"/>
      <title>JSnap Web Console</title>
    <script type="text/javascript">
    &lt;!--
    function init() {
      if (top.location.href != self.location.href)
        top.location.href = self.location.href;
    }
    // --&gt;
    </script>
    </head>
    <body onload="init();">
    <form method="post" action="/login.do">
    <center>
    <table style="border:solid 2px;border-color:#800000" bgcolor="#eeeeee" cellpadding="4" cellspacing="8">
      <tr><td align="center"><b>JSnap Web Console</b></td></tr>
      <tr><td align="center">Administration and Statistics</td></tr>
      <xsl:if test="error != ''">
        <tr><td class="error" align="center"><xsl:value-of select="error"/></td></tr>
      </xsl:if>
      <tr><td align="center">
      <table cellpadding="4">
        <tr>
        <td><i>Username:</i></td><td><input class="text" type="text" name="username" value="{username}"/></td>
        </tr>
        <tr>
        <td><i>Password:</i></td><td><input class="text" type="password" name="password" value="{password}"/></td>
        </tr>
        <tr>
        <td colspan="2" align="center"><input class="button" type="submit" value="Login"/></td>
        </tr>
      </table>
      </td></tr>
    </table>
    </center>
    </form>
    </body>
    </html>
  </xsl:template>
</xsl:stylesheet>
