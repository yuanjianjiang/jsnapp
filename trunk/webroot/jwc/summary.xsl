<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:template match="/summary">
  <html>
  <head>
    <meta http-equiv="refresh" content="60"/>
    <link rel="stylesheet" type="text/css" href="/style.css"/>
    <title>JSnap Web Console</title>
  </head>
  <body>
    <p>Reported on: <i><xsl:value-of select="now"/></i></p>
    <p><b>Databases:</b></p>
    <p>
    <xsl:for-each select="database">
    <xsl:sort select="name"/>
    <xsl:choose>
      <xsl:when test="status='online'">
      <b><font color="#00aa00"><xsl:value-of select="name"/></font></b>
      (online)
      <br/>
      </xsl:when>
      <xsl:when test="status='offline'">
      <b><font color="#dd0000"><xsl:value-of select="name"/></font></b>
      (offline until <xsl:value-of select="whenAvailable"/>)
      <br/>
      </xsl:when>
    </xsl:choose>
    </xsl:for-each>
    </p>
    <p><b>Listeners:</b></p>
    <p>
    <xsl:for-each select="listener">
    <xsl:sort data-type="number" select="port"/>
    on port <i><xsl:value-of select="port"/></i>: accepts <xsl:value-of select="accepts"/><br/>
    </xsl:for-each>
    </p>
  </body>
  </html>
  </xsl:template>
</xsl:stylesheet>
