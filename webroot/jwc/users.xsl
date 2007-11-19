<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="html"
    indent="yes"
    omit-xml-declaration="no"
    doctype-public="-//W3C//DTD XHTML 1.0 Strict//EN"
    doctype-system="http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"
  />
  <xsl:template match="/users">
  <html>
  <head>
    <title>JSnap Web Console</title>
    <link rel="stylesheet" type="text/css" href="/style.css"/>
    <link rel="stylesheet" type="text/css" href="/jwc/jqmodal.css"/>
    <script type="text/javascript" src="/jwc/commons.js"/>
    <script type="text/javascript" src="/jwc/jquery.js"></script>
    <script type="text/javascript" src="/jwc/jqmodal.js"></script>
    <script type="text/javascript">
    &lt;!--
    function showAlgoModify() {
      visible('inputalgo');
      visible('donealgo');
      visible('cancelalgo');
    }

    function hideAlgoModify() {
      invisible('inputalgo');
      invisible('donealgo');
      invisible('cancelalgo');
    }

    function keypressedModify(event) {
      var key = (event.keyCode ? event.keyCode : (event.which ? event.which : event.charCode));
      if (key == 13)
        document.modifyalgo.submit();
      else if (key == 27)
        hideAlgoModify();
    }

    function doCheckboxDisable(state) {
      <xsl:for-each select="user">
      	getElement('chk<xsl:value-of select="name"/>').disabled = state;
      </xsl:for-each>
    }

    function checkboxDisable(state) {
      doCheckboxDisable(state);
      checkboxSetAll(state);
      if (state) {
        invisible('chksetall');
        visible('chksetallinactive');
      } else {
        visible('chksetall');
        invisible('chksetallinactive');
      }
    }

    function checkboxSetAll(state) {
      <xsl:for-each select="user">
      	getElement('chk<xsl:value-of select="name"/>').checked = state;
      </xsl:for-each>
    }

    function confirmDelete() {
      if (confirm('Selected users will be deleted.\r\nAre you sure?')) {
        doCheckboxDisable(false);
        document.userop.operation.value='delete';
        document.userop.submit();
      }
    }

    function init() {
      <xsl:choose>
        <xsl:when test="mdalg/show = 'true'">
          invisible('mdalgshow');
          hideAlgoModify();
        </xsl:when>
        <xsl:otherwise>
          invisible('mdalginfo');
          invisible('mdalgmodify');
          invisible('mdalgdiff');
        </xsl:otherwise>
      </xsl:choose>
      <xsl:choose>
        <xsl:when test="create != ''">
          invisible('createshow');
        </xsl:when>
        <xsl:otherwise>
          invisible('createtable');
        </xsl:otherwise>
      </xsl:choose>
      <xsl:call-template name="criteriainit">
      <xsl:with-param name="name">username</xsl:with-param>
      </xsl:call-template>
      <xsl:call-template name="criteriainit">
      <xsl:with-param name="name">group</xsl:with-param>
      </xsl:call-template>
      <xsl:call-template name="criteriainit">
      <xsl:with-param name="name">db</xsl:with-param>
      </xsl:call-template>
      <xsl:call-template name="criteriainit">
      <xsl:with-param name="name">account</xsl:with-param>
      </xsl:call-template>
      <xsl:call-template name="criteriainit">
      <xsl:with-param name="name">rights</xsl:with-param>
      </xsl:call-template>
      <xsl:call-template name="criteriainit">
      <xsl:with-param name="name">ipaddr</xsl:with-param>
      </xsl:call-template>
      <xsl:if test="total &gt; 0">
        invisible('allselected');
        invisible('chksetallinactive');
      </xsl:if>
    }
    // --&gt;
    </script>
  </head>
  <body onload="init();">
    <form name="modifyalgo" method="post" action="main.do">
    <input type="hidden" name="page" value="{key}"/>
    <input type="hidden" name="operation" value="mdalgmodify"/>
    <table class="info" width="100%">
      <tr><td>
        <table id="mdalgshow" width="100%" cellspacing="0" cellpadding="4">
          <tr><td><a href="#" onclick="invisible('mdalgshow');visible('mdalginfo');visible('mdalgmodify');hideAlgoModify();visible('mdalgdiff');return false;"><small>View how user passwords are stored</small></a></td></tr>
        </table>
        <table id="mdalginfo" width="100%" cellspacing="0" cellpadding="4">
          <xsl:if test="mdalg/error != ''">
            <tr><td class="error"><xsl:value-of select="mdalg/error"/>.</td></tr>
          </xsl:if>
          <xsl:if test="mdalg/algo != ''">
            <xsl:choose>
            <xsl:when test="mdalg/algo = 'PLAIN'">
              <tr><td>User passwords are stored in plaintext. <span style="text-decoration:blink">This is very insecure.</span> Please use something like SHA-1, MD5 etc.</td></tr>
            </xsl:when>
            <xsl:otherwise>
              <tr><td>The message digest algorithm used for storing user passwords is <b><xsl:value-of select="mdalg/algo"/></b>.</td></tr>
            </xsl:otherwise>
            </xsl:choose>
          </xsl:if>
        </table>
        <table id="mdalgmodify" cellspacing="0" cellpadding="4">
          <tr>
            <td><a href="#" onclick="showAlgoModify();setFocus('inputalgo');return false;"><small>[modify algorithm]</small></a></td>
            <td><input class="inline" id="inputalgo" type="text" name="algo" value="{mdalg/algo}" onkeypress="keypressedModify(event);"/></td>
            <td><a id="donealgo" href="#" onclick="document.modifyalgo.submit();"><small>[done]</small></a></td>
            <td><a id="cancelalgo" href="#" onclick="hideAlgoModify();return false;"><small>[cancel]</small></a></td>
          </tr>
        </table>
        <table id="mdalgdiff" cellspacing="0" cellpadding="4">
          <xsl:if test="mdalg/different &gt; 0">
            <xsl:choose>
            <xsl:when test="mdalg/different = 1">
              <tr><td>There is <b>1 active password</b> that is stored with a different message digest algorithm.<br/>
              Click <a href="main.do?page={key}&amp;operation=qyalgodiff&amp;refresh=true">here</a> to see the user who owns that password.</td></tr>
            </xsl:when>
            <xsl:when test="mdalg/different &gt; 1">
              <tr><td>There are <b><xsl:value-of select="mdalg/different"/> active passwords</b> that are stored with a different message digest algorithm.<br/>
              Click <a href="main.do?page={key}&amp;operation=qyalgodiff&amp;refresh=true">here</a> to see the users who own those passwords.</td></tr>
            </xsl:when>
            </xsl:choose>
          </xsl:if>
          <tr><td><a href="#" onclick="visible('mdalgshow');invisible('mdalginfo');invisible('mdalgmodify');invisible('mdalgdiff');return false;"><small>Hide</small></a></td></tr>
        </table>
      </td></tr>
    </table>
    </form>
    <form method="post" action="main.do">
    <input type="hidden" name="page" value="{key}"/>
    <input type="hidden" name="operation" value="create"/>
    <table class="darkgray" width="100%" cellspacing="0" cellpadding="4">
    <tr>
      <td>
      <table id="createshow">
        <tr><td><a href="#" onclick="visible('createtable');invisible('createshow');return false;"><small>Create a new user</small></a></td></tr>
      </table>
      <table id="createtable">
      <xsl:if test="create/error != ''">
        <tr><td class="error" colspan="3"><xsl:value-of select="create/error"/></td></tr>
      </xsl:if>
      <xsl:if test="create/message != ''">
        <tr><td class="notify" colspan="3"><xsl:value-of select="create/message"/></td></tr>
      </xsl:if>
      <tr>
        <td><b>Username:</b></td>
        <td><input class="text" type="text" name="newname" value="{newname}"/></td>
        <td><input class="button" type="submit" value="Create"/></td>
      </tr>
      <tr>
        <td><b>Password:</b></td>
        <td><input class="text" type="password" name="password" value=""/></td>
        <td></td>
      </tr>
      <tr>
        <td></td>
        <td colspan="2"><input class="checkbox" type="checkbox" name="admin"/><label>Administrator</label></td>
      </tr>
      <tr>
        <td></td>
        <td colspan="2"><input class="checkbox" type="checkbox" name="pwdexpired" checked="true"/><label>Password expired</label></td>
      </tr>
      <tr>
        <td><a id="hidelink" href="#" onclick="invisible('createtable');visible('createshow');return false;"><small>Hide</small></a></td>
        <td colspan="2"><input class="checkbox" type="checkbox" name="lockedout"/><label>Locked out</label></td>
      </tr>
      </table>
      </td>
    </tr>
    </table>
    </form>
    <form name="query" method="post" action="main.do">
    <input type="hidden" name="page" value="{key}"/>
    <input type="hidden" name="refresh" value="true"/>
    <table class="lightgray" width="100%" cellspacing="0" cellpadding="4">
      <tr><td colspan="2">
      <table>
        <tr>
          <td><b>Records Per Page:</b></td>
          <td>
          <select name="perpage">
            <xsl:call-template name="option">
              <xsl:with-param name="tested"><xsl:value-of select="perpage"/></xsl:with-param>
              <xsl:with-param name="key">10</xsl:with-param>
              <xsl:with-param name="text">10</xsl:with-param>
            </xsl:call-template>
            <xsl:call-template name="option">
              <xsl:with-param name="tested"><xsl:value-of select="perpage"/></xsl:with-param>
              <xsl:with-param name="key">20</xsl:with-param>
              <xsl:with-param name="text">20</xsl:with-param>
            </xsl:call-template>
            <xsl:call-template name="option">
              <xsl:with-param name="tested"><xsl:value-of select="perpage"/></xsl:with-param>
              <xsl:with-param name="key">50</xsl:with-param>
              <xsl:with-param name="text">50</xsl:with-param>
            </xsl:call-template>
          </select>
          </td>
        </tr>
      </table>
      </td></tr>
      <tr><td>
      <xsl:call-template name="criteriaheader">
        <xsl:with-param name="count"><xsl:value-of select="count(username[@selected='true'])"/></xsl:with-param>
        <xsl:with-param name="name">username</xsl:with-param>
        <xsl:with-param name="caption">by Username</xsl:with-param>
      </xsl:call-template>
      <table id="usernametable" cellspacing="0" cellpadding="4">
      <tr>
        <td><a href="#" onclick="document.query.usernamestate.value='off';invisible('usernametable');visible('usernamelink');return false;"><small>Hide</small></a></td>
        <td><b>Username:</b></td>
        <td><input class="text" type="text" name="username" value="{username}"/></td>
        <td><i>Use wildcard (*) if necessary</i></td>
      </tr>
      </table>
      </td></tr>
      <tr><td>
      <xsl:call-template name="criteriaheader">
        <xsl:with-param name="count"><xsl:value-of select="count(groups/group[@selected='true'])"/></xsl:with-param>
        <xsl:with-param name="name">group</xsl:with-param>
        <xsl:with-param name="caption">by Group membership</xsl:with-param>
      </xsl:call-template>
      <table id="grouptable" cellspacing="0" cellpadding="4">
      <tr>
        <td valign="top"><a href="#" onclick="document.query.groupstate.value='off';invisible('grouptable');visible('grouplink');return false;"><small>Hide</small></a></td>
        <td valign="top"><b>Member of:</b></td>
        <td valign="top">
          <select name="group" multiple="true" size="4">
            <xsl:for-each select="groups/group">
              <xsl:call-template name="listoption">
                <xsl:with-param name="selected"><xsl:value-of select="@selected"/></xsl:with-param>
                <xsl:with-param name="key"><xsl:value-of select="id"/></xsl:with-param>
                <xsl:with-param name="text"><xsl:value-of select="name"/></xsl:with-param>
              </xsl:call-template>
            </xsl:for-each>
          </select>
        </td>
      </tr>
      </table>
      </td></tr>
      <tr><td>
      <xsl:call-template name="criteriaheader">
        <xsl:with-param name="count"><xsl:value-of select="count(databases/database[@selected='true'])"/></xsl:with-param>
        <xsl:with-param name="name">db</xsl:with-param>
        <xsl:with-param name="caption">by Database access</xsl:with-param>
      </xsl:call-template>
      <table id="dbtable" cellspacing="0" cellpadding="4">
      <tr>
        <td valign="top"><a href="#" onclick="document.query.dbstate.value='off';invisible('dbtable');visible('dblink');return false;"><small>Hide</small></a></td>
        <td valign="top"><b>Accessed:</b></td>
        <td valign="top">
          <select name="db" multiple="true" size="4">
            <xsl:for-each select="databases/database">
              <xsl:call-template name="listoption">
                <xsl:with-param name="selected"><xsl:value-of select="@selected"/></xsl:with-param>
                <xsl:with-param name="key"><xsl:value-of select="."/></xsl:with-param>
                <xsl:with-param name="text"><xsl:value-of select="."/></xsl:with-param>
              </xsl:call-template>
            </xsl:for-each>
          </select>
        </td>
      </tr>
      </table>
      </td></tr>
      <tr><td>
      <xsl:call-template name="criteriaheader">
        <xsl:with-param name="count"><xsl:value-of select="count(status/*[@selected='true'])"/></xsl:with-param>
        <xsl:with-param name="name">account</xsl:with-param>
        <xsl:with-param name="caption">by Account status</xsl:with-param>
      </xsl:call-template>
      <table id="accounttable" cellspacing="0" cellpadding="4">
      <tr>
        <td><a href="#" onclick="document.query.accountstate.value='off';invisible('accounttable');visible('accountlink');return false;"><small>Hide</small></a></td>
        <td><b>Status:</b></td>
        <td><xsl:call-template name="checkbox">
              <xsl:with-param name="selected"><xsl:value-of select="status/active/@selected"/></xsl:with-param>
              <xsl:with-param name="name">active</xsl:with-param>
            </xsl:call-template>Active</td>
      </tr>
      <tr>
        <td></td>
        <td></td>
        <td><xsl:call-template name="checkbox">
              <xsl:with-param name="selected"><xsl:value-of select="status/expired/@selected"/></xsl:with-param>
              <xsl:with-param name="name">expired</xsl:with-param>
            </xsl:call-template>Password expired</td>
      </tr>
      <tr>
        <td></td>
        <td></td>
        <td><xsl:call-template name="checkbox">
              <xsl:with-param name="selected"><xsl:value-of select="status/locked/@selected"/></xsl:with-param>
              <xsl:with-param name="name">locked</xsl:with-param>
            </xsl:call-template>Account locked</td>
      </tr>
      </table>
      </td></tr>
      <tr><td>
      <xsl:call-template name="criteriaheader">
        <xsl:with-param name="count"><xsl:value-of select="count(rights/*[@selected='true'])"/></xsl:with-param>
        <xsl:with-param name="name">rights</xsl:with-param>
        <xsl:with-param name="caption">by Administrative rights</xsl:with-param>
      </xsl:call-template>
      <table id="rightstable" cellspacing="0" cellpadding="4">
      <tr>
        <td><a href="#" onclick="document.query.rightsstate.value='off';invisible('rightstable');visible('rightslink');return false;"><small>Hide</small></a></td>
        <td><b>Rights:</b></td>
        <td><xsl:call-template name="checkbox">
              <xsl:with-param name="selected"><xsl:value-of select="rights/user/@selected"/></xsl:with-param>
              <xsl:with-param name="name">adminuser</xsl:with-param>
            </xsl:call-template>Administrator as a user</td>
      </tr>
      <tr>
        <td></td>
        <td></td>
        <td><xsl:call-template name="checkbox">
              <xsl:with-param name="selected"><xsl:value-of select="rights/group/@selected"/></xsl:with-param>
              <xsl:with-param name="name">admingroup</xsl:with-param>
            </xsl:call-template>Administrator from a group membership</td>
      </tr>
      <tr>
        <td></td>
        <td></td>
        <td><xsl:call-template name="checkbox">
              <xsl:with-param name="selected"><xsl:value-of select="rights/ordinary/@selected"/></xsl:with-param>
              <xsl:with-param name="name">ordinaryuser</xsl:with-param>
            </xsl:call-template>Ordinary user</td>
      </tr>
      </table>
      </td></tr>
      <tr><td>
      <xsl:call-template name="criteriaheader">
        <xsl:with-param name="count"><xsl:value-of select="count(ipaddr[@selected='true'])"/></xsl:with-param>
        <xsl:with-param name="name">ipaddr</xsl:with-param>
        <xsl:with-param name="caption">by Originating IP address</xsl:with-param>
      </xsl:call-template>
      <table id="ipaddrtable" cellspacing="0" cellpadding="4">
      <tr>
        <td><a href="#" onclick="document.query.ipaddrstate.value='off';invisible('ipaddrtable');visible('ipaddrlink');return false;"><small>Hide</small></a></td>
        <td><b>IP Address:</b></td>
        <td><input class="text" type="text" name="ipaddr" value="{ipaddr}"/></td>
        <td><i>Use wildcard (*) if necessary</i></td>
      </tr>
      </table>
      </td></tr>
      <tr><td colspan="2"><input class="button" type="submit" value="List"/></td></tr>
      <tr><td colspan="2" align="right" bgcolor="#ffffff">Reported on: <i><xsl:value-of select="now"/></i></td></tr>
    </table>
    </form>

    <xsl:choose>
      <xsl:when test="total &gt; 0">
        <form name="userop" action="main.do">
        <input type="hidden" name="page" value="{key}"/>
        <input type="hidden" name="operation" value=""/>
        <table width="100%">
        <tr>
        <td class="lightgray" valign="top" width="30%" rowspan="2">
        <xsl:variable name="listed" select="count(user)"/>
        <table width="100%" cellspacing="0" cellpadding="4">
          <tr><td colspan="2"><b>Perform an operation on users:</b></td></tr>
          <xsl:if test="$listed!=total">
          <tr><td colspan="2"><input class="radio" type="radio" name="selection" value="all" onclick="checkboxDisable(true);visible('allselected');invisible('chksetallinactive');"/>All of the matching (<xsl:value-of select="total"/> user<xsl:if test="total&gt;1">s</xsl:if>)</td></tr>
          </xsl:if>
          <tr><td colspan="2"><input class="radio" type="radio" name="selection" value="selected" onclick="checkboxDisable(true);invisible('allselected');"/>Listed on this page (<xsl:value-of select="$listed"/> user<xsl:if test="$listed&gt;1">s</xsl:if>)</td></tr>
          <tr><td colspan="2"><input class="radio" type="radio" name="selection" value="selected" onclick="checkboxDisable(false);invisible('allselected');" checked="true"/>Selected</td></tr>
          <tr>
          <td align="center"><a href="#" onclick="confirmDelete();"><small>[delete]</small></a></td>
          <td align="center"><a href="#" onclick="doCheckboxDisable(false);document.userop.operation.value='grant';document.userop.submit();"><small>[grant privileges]</small></a></td>
          </tr>
          <tr>
          <td align="center"><a href="#" onclick="doCheckboxDisable(false);document.userop.operation.value='lockout';document.userop.submit();"><small>[lock out]</small></a></td>
          <td align="center"><a href="#" onclick="doCheckboxDisable(false);document.userop.operation.value='revoke';document.userop.submit();"><small>[revoke privileges]</small></a></td>
          </tr>
          <tr>
          <td align="center"><a href="#" onclick="doCheckboxDisable(false);document.userop.operation.value='unlock';document.userop.submit();"><small>[unlock]</small></a></td>
          <td align="center"><a href="#" onclick="doCheckboxDisable(false);document.userop.operation.value='expire';document.userop.submit();"><small>[expire passwords]</small></a></td>
          </tr>
          <tr>
          <td align="center"></td>
          <td align="center"><a href="#" onclick="doCheckboxDisable(false);document.userop.operation.value='reset';document.userop.submit();"><small>[reset passwords]</small></a></td>
          </tr>
        </table>
        </td>
        <td>
        <xsl:if test="operation/message!=''">
        <table><tr><td class="notify"><xsl:value-of select="operation/message"/></td></tr></table>
        </xsl:if>
        <xsl:if test="operation/error!=''">
        <table><tr><td class="error"><xsl:value-of select="operation/error"/></td></tr></table>
        </xsl:if>
        <table><tr><td>
        <xsl:choose>
          <xsl:when test="total = 1">
            Found a single user.
          </xsl:when>
          <xsl:otherwise>
            Found <xsl:value-of select="total"/> users.
          </xsl:otherwise>
        </xsl:choose>
        </td></tr></table>
        </td>
        </tr>
        <tr>
        <td valign="top" width="70%">
        <table id="chksetall">
          <tr><td><a href="#" onclick="checkboxSetAll(true);return false;"><small>[all]</small></a></td>
          <td><a href="#" onclick="checkboxSetAll(false);return false;"><small>[none]</small></a></td></tr>
        </table>
        <table id="chksetallinactive">
          <tr><td><small>[all]</small></td>
          <td><small>[none]</small></td></tr>
        </table>
        <table id="allselected">
          <tr><td class="notify">Users on all pages are selected. (<xsl:value-of select="total"/> user<xsl:if test="total&gt;1">s</xsl:if> in total.)</td></tr>
        </table>
        <table cellspacing="0" cellpadding="4">
          <tr>
            <td></td>
            <td><b>Username</b></td>
            <td><b>Administrator</b></td>
            <td align="center"><b>Password expired</b></td>
            <td align="center"><b>Locked out</b></td>
            <td></td>
          </tr>
          <xsl:for-each select="user">
          <xsl:sort select="admin" order="descending"/>
          <xsl:sort select="name"/>
            <input type="hidden" name="listeduser" value="{name}"/>
            <tr>
              <td><input class="checkbox" id="chk{name}" type="checkbox" name="{name}"/></td>
              <td><xsl:value-of select="name"/></td>
              <td><xsl:value-of select="admin"/></td>
              <td align="center"><xsl:value-of select="expired"/></td>
              <td align="center"><xsl:value-of select="locked"/></td>
              <td>

              <a href="#" onclick="return false;" class="groupstrigger"><small>[manage groups]</small></a>
              <script type="text/javascript">
                $().ready(function() {
	              $('#pleasewait').jqm({ajax:'/jwc/main.do?page=<xsl:value-of select="../key"/>&amp;current=<xsl-value-of select="../current"/>&amp;runonce=true&amp;operation=viewgroups&amp;username=<xsl:value-of select="name"/>',trigger:'a.groupstrigger'});
                });
              </script>

              <!--a class="groupstrigger" href="/jwc/main.do?page={../key}&amp;current={../current}&amp;runonce=true&amp;operation=viewgroups&amp;username={name}"><small>[manage groups]</small></a>
              <script type="text/javascript">
                $().ready(function() {
	              $('#pleasewait').jqm({ajax:'@href',trigger:'a.groupstrigger'});
                });
              </script>
              <div class="jqmWindow" id="pleasewait">
              <table><tr><td><img src="/jwc/busy.gif" alt="loading"/></td><td>Loading, please wait...</td></tr></table>
              </div-->

              </td>
            </tr>
          </xsl:for-each>
        </table>
        </td>
        </tr>
        </table>
        </form>
        <div class="jqmWindow" id="pleasewait">
        <table><tr><td><img src="/jwc/busy.gif" alt="loading"/></td><td>Loading, please wait...</td></tr></table>
        </div>
      </xsl:when>
      <xsl:otherwise>
        <table><tr><td>No users matched the search criteria.</td></tr></table>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:if test="pages&gt;1">
      <xsl:call-template name="pages"/>
    </xsl:if>
  </body>
  </html>
  </xsl:template>

  <xsl:template name="pages">
    <table width="100%">
    <tr>
      <td align="left" width="33%">
        <xsl:if test="current &gt; 1">
          <form method="post" action="main.do">
          <input type="hidden" name="page" value="{key}"/>
          <input type="hidden" name="current" value="{current - 1}"/>
          <input class="button" type="submit" value="Previous"/>
          </form>
        </xsl:if>
      </td>
      <td align="center" width="33%">
        <form method="post" action="main.do">
        Pages:
        <select name="current">
          <xsl:call-template name="recursion">
            <xsl:with-param name="i">1</xsl:with-param>
            <xsl:with-param name="n"><xsl:value-of select="pages"/></xsl:with-param>
            <xsl:with-param name="selected"><xsl:value-of select="current"/></xsl:with-param>
          </xsl:call-template>
        </select>
        <input type="hidden" name="page" value="{key}"/>
        <input class="button" type="submit" value="Go"/>
        </form>
      </td>
      <td align="right" width="34%">
        <xsl:if test="current &lt; pages">
          <form method="post" action="main.do">
          <input type="hidden" name="page" value="{key}"/>
          <input type="hidden" name="current" value="{current + 1}"/>
          <input class="button" type="submit" value="Next"/>
          </form>
        </xsl:if>
      </td>
    </tr>
    </table>
  </xsl:template>

  <xsl:template name="recursion">
    <xsl:param name="i"/>
    <xsl:param name="n"/>
    <xsl:param name="selected"/>
    <xsl:call-template name="option">
      <xsl:with-param name="tested"><xsl:value-of select="$selected"/></xsl:with-param>
      <xsl:with-param name="key"><xsl:value-of select="$i"/></xsl:with-param>
      <xsl:with-param name="text"><xsl:value-of select="$i"/></xsl:with-param>
    </xsl:call-template>
    <xsl:if test="$i &lt; $n">
      <xsl:call-template name="recursion">
        <xsl:with-param name="i"><xsl:value-of select="$i + 1"/></xsl:with-param>
        <xsl:with-param name="n"><xsl:value-of select="$n"/></xsl:with-param>
        <xsl:with-param name="selected"><xsl:value-of select="$selected"/></xsl:with-param>
      </xsl:call-template>
    </xsl:if>
  </xsl:template>

  <xsl:template name="option">
    <xsl:param name="tested"/>
    <xsl:param name="key"/>
    <xsl:param name="text"/>
    <xsl:choose>
      <xsl:when test="$tested = $key">
        <option value="{$key}" selected="true"><xsl:value-of select="$text"/></option>
      </xsl:when>
      <xsl:otherwise>
        <option value="{$key}"><xsl:value-of select="$text"/></option>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="listoption">
    <xsl:param name="selected"/>
    <xsl:param name="key"/>
    <xsl:param name="text"/>
    <xsl:choose>
      <xsl:when test="$selected='true'">
        <option value="{$key}" selected="true"><xsl:value-of select="$text"/></option>
      </xsl:when>
      <xsl:otherwise>
        <option value="{$key}"><xsl:value-of select="$text"/></option>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="checkbox">
    <xsl:param name="selected"/>
    <xsl:param name="name"/>
    <xsl:choose>
      <xsl:when test="$selected='true'">
        <input class="checkbox" type="checkbox" name="{$name}" checked="true"/>
      </xsl:when>
      <xsl:otherwise>
        <input class="checkbox" type="checkbox" name="{$name}"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="criteriainit">
    <xsl:param name="name"/>
    if (document.query.<xsl:value-of select="$name"/>state.value == 'on')
      invisible('<xsl:value-of select="$name"/>link');
    else
      invisible('<xsl:value-of select="$name"/>table');
  </xsl:template>

  <xsl:template name="criteriaheader">
    <xsl:param name="count"/>
    <xsl:param name="name"/>
    <xsl:param name="caption"/>
    <xsl:choose>
      <xsl:when test="$count = 0">
        <input type="hidden" name="{$name}state" value="off"/>
      </xsl:when>
      <xsl:otherwise>
        <input type="hidden" name="{$name}state" value="on"/>
      </xsl:otherwise>
    </xsl:choose>
    <table id="{$name}link">
    <tr>
      <td><a href="#" onclick="document.query.{$name}state.value='on';invisible('{$name}link');visible('{$name}table');return false;"><small><xsl:value-of select="$caption"/></small></a></td>
    </tr>
    </table>
  </xsl:template>
</xsl:stylesheet>
