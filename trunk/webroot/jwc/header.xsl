<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:template match="/pages">
  <html>
  <head>
    <link rel="stylesheet" type="text/css" href="/style.css"/>
    <title>JSnap Web Console</title>
  </head>
  <body class="header">
  <form method="post" action="/jwc/main.do" target="bottom">
  <input type="hidden" name="refresh" value="true"/>
  <table cellspacing="0" cellpadding="4" width="100%">
    <tr>
      <td>
      <xsl:if test="count(item) &gt; 0">
      <b class="light">Available pages:</b>
      </xsl:if>
      </td>
      <td width="100%" align="right">
      <table>
        <tr>
          <td>
          <b><a class="light" href="/jwc/main.do" target="bottom">Home</a></b>
          </td>
          <td width="3">
          </td>
          <td>
          <b><a class="light" href="/renew.xml" target="_top">Renew Password</a></b>
          </td>
          <td width="3">
          </td>
          <td>
          <b><a class="light" href="/jwc/logout.xml" target="_top">Logout</a></b>
          </td>
        </tr>
      </table>
      </td>
    </tr>
    <tr>
      <td>
      <xsl:if test="count(item) &gt; 0">
      <select name="page">
        <xsl:variable name="categories" select="//category[not(.=following::category)]"/>
        <xsl:for-each select="$categories">
          <xsl:sort select="."/>
          <xsl:variable name="current" select="."/>
          <!--option><xsl:value-of select="$current"/></option-->
          <xsl:for-each select="//item[category=$current]">
            <xsl:sort select="name"/>
            <option value="{key}"><xsl:value-of select="category"/>: <xsl:value-of select="name"/></option>
          </xsl:for-each>
        </xsl:for-each>
      </select>
      </xsl:if>
      </td>
      <td>
      <xsl:if test="count(item) &gt; 0">
      <input class="button" type="submit" value="View"/>
      </xsl:if>
      </td>
    </tr>
  </table>
  </form>
  </body>
  </html>
  </xsl:template>
</xsl:stylesheet>
