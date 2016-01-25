package im.tox.tox4j.testing.autotest

import im.tox.tox4j.TestConstants._
import im.tox.tox4j.core.options.ProxyOptions
import im.tox.tox4j.core.{ToxCore, ToxCoreFactory}
import im.tox.tox4j.{SocksServer, ToxCoreTestBase}
import org.scalatest.concurrent.Timeouts
import org.scalatest.exceptions.TestFailedDueToTimeoutException
import org.scalatest.time.SpanSugar._

import scala.language.postfixOps

abstract class AliceBobTest extends AliceBobTestBase with Timeouts {

  protected def ignoreTimeout = false

  protected def enableUdp = true
  protected def enableTcp = false
  protected def enableIpv4 = true
  protected def enableIpv6 = false
  protected def enableHttp = false
  protected def enableSocks = false

  private def withBootstrappedTox(
    ipv6Enabled: Boolean,
    udpEnabled: Boolean,
    proxyOptions: ProxyOptions = ProxyOptions.None
  )(
    f: ToxCore[ChatState] => Unit
  ): Unit = {
    ToxCoreFactory.withToxS[ChatState, Unit](ipv6Enabled, udpEnabled, proxyOptions) { tox =>
      bootstrap(ipv6Enabled, udpEnabled, tox)
      f(tox)
    }
  }

  private def runAliceBobTest_Direct(withTox: => (ToxCore[ChatState] => Unit) => Unit): Unit = {
    failAfter(Timeout millis) {
      runAliceBobTest(withTox)
    }
  }

  private def runAliceBobTest_Socks(ipv6Enabled: Boolean, udpEnabled: Boolean): Unit = {
    val proxy = SocksServer.withServer { proxy =>
      failAfter(Timeout millis) {
        runAliceBobTest(withBootstrappedTox(ipv6Enabled, udpEnabled, new ProxyOptions.Socks5(proxy.getAddress, proxy.getPort)))
      }
      proxy
    }
    if (!udpEnabled) {
      assert(proxy.getAccepted == 2)
    }
  }

  test("UDP4") {
    assume(enableUdp)
    assume(enableIpv4)
    try {
      runAliceBobTest_Direct(ToxCoreFactory.withToxS[ChatState, Unit](ipv6Enabled = false, udpEnabled = true))
    } catch {
      case e: TestFailedDueToTimeoutException if ignoreTimeout =>
        cancel(s"Test timed out after $Timeout millis", e)
    }
  }

  test("UDP6") {
    assume(enableUdp)
    assume(enableIpv6)
    failAfter(Timeout millis) {
      runAliceBobTest_Direct(ToxCoreFactory.withToxS(ipv6Enabled = true, udpEnabled = true))
    }
  }

  test("TCP4") {
    assume(enableTcp)
    assume(enableIpv4)
    assume(ToxCoreTestBase.checkIPv4.isEmpty)
    failAfter(Timeout millis) {
      runAliceBobTest_Direct(withBootstrappedTox(ipv6Enabled = false, udpEnabled = false))
    }
  }

  test("TCP6") {
    assume(enableTcp)
    assume(enableIpv6)
    assume(ToxCoreTestBase.checkIPv6.isEmpty)
    failAfter(Timeout millis) {
      runAliceBobTest_Direct(withBootstrappedTox(ipv6Enabled = true, udpEnabled = false))
    }
  }

  test("UDP4+SOCKS5") {
    assume(enableUdp)
    assume(enableIpv4)
    assume(enableSocks)
    assume(ToxCoreTestBase.checkIPv4.isEmpty)
    runAliceBobTest_Socks(ipv6Enabled = false, udpEnabled = true)
  }

  test("UDP6+SOCKS5") {
    assume(enableUdp)
    assume(enableIpv6)
    assume(enableSocks)
    assume(ToxCoreTestBase.checkIPv6.isEmpty)
    runAliceBobTest_Socks(ipv6Enabled = true, udpEnabled = true)
  }

  test("TCP4+SOCKS5") {
    assume(enableTcp)
    assume(enableIpv4)
    assume(enableSocks)
    assume(ToxCoreTestBase.checkIPv4.isEmpty)
    runAliceBobTest_Socks(ipv6Enabled = false, udpEnabled = false)
  }

  test("TCP6+SOCKS5") {
    assume(enableTcp)
    assume(enableIpv6)
    assume(enableSocks)
    assume(ToxCoreTestBase.checkIPv6.isEmpty)
    runAliceBobTest_Socks(ipv6Enabled = true, udpEnabled = false)
  }

}