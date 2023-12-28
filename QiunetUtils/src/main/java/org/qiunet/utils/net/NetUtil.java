package org.qiunet.utils.net;

import com.google.common.net.InetAddresses;
import org.qiunet.utils.common.CommonUtil;
import org.qiunet.utils.exceptions.CustomException;
import org.qiunet.utils.logger.LoggerType;
import org.qiunet.utils.string.StringUtil;
import org.qiunet.utils.thread.ThreadPoolManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.stream.Stream;


/***
 *
 * @Author qiunet
 * @Date Create in 2018/6/22 12:04
 **/
public class NetUtil {

	// Max hex digits in each IPv6 group
	private static final int IPV6_MAX_HEX_DIGITS_PER_GROUP = 4;
	// Max number of hex groups (separated by :) in an IPV6 address
	private static final int IPV6_MAX_HEX_GROUPS = 8;

	private static final int MAX_UNSIGNED_SHORT = 0xffff;
	/**
	 * 默认最大端口，65535
	 */
	public static final int PORT_RANGE_MAX = 0xFFFF;

	/**
	 * 默认最小端口，1024
	 */
	public static final int PORT_RANGE_MIN = 1024;

	private static final int MAX_BYTE = 128;

	private static final int BASE_16 = 16;
	/**
	 * 是否是本机ip
	 *
	 * 本机也返回true
	 * @return
	 */
	public static boolean isLocalIp(String ip){
		return CommonUtil.existInList(ip, "localhost", "0:0:0:0:0:0:0:1", "127.0.0.1");
	}

	/**
	 * 是否是内网IP
	 * @return
	 */
	public static boolean isInnerIp(String ip){
		InetAddress inetAddress = InetAddresses.forString(ip);
		return isInnerIp(inetAddress);
	}
	/**
	 * 是否是内网IP
	 * @return
	 */
	public static boolean isInnerIp(InetAddress inetAddress){
		return inetAddress.isSiteLocalAddress() || inetAddress.isLoopbackAddress();
	}

	/***
	 * 得到内网ip v4
	 * @return
	 */
	public static String getInnerIp() {
		return localIpv4s().stream().filter(str -> ! isLocalIp(str)).findFirst().orElse(null);
	}
	/**
	 * 是否为有效的端口<br>
	 * 此方法并不检查端口是否被占用
	 *
	 * @param port 端口号
	 * @return 是否有效
	 */
	public static boolean isValidPort(int port) {
		// 有效端口是0～65535
		return port >= 0 && port <= PORT_RANGE_MAX;
	}
	/**
	 * 得到外网IP6
	 * @return
	 */
	public static String getPublicIp6(){
		LinkedHashSet<String> strings = localIpv6s();
		for (String string : strings) {
			if (string.startsWith("0:0:0:0:0:0:0:1")
			|| string.startsWith("fe80") || string.startsWith("fE80")
			|| string.startsWith("Fc00") || string.startsWith("FC00")
			|| string.startsWith("Fec0") || string.startsWith("FEC0")
			) {
				continue;
			}
			String ip = StringUtil.split(string, "%")[0];
			LoggerType.DUODUO_FLASH_HANDLER.error("Use ipv6 address: {}", ip);
			return ip;
		}
		throw new CustomException("Not support ipv6!");
	}
	/**
	 * 得到外网IP
	 * @return
	 */
	public static String getPublicIp4(){
		LinkedHashSet<InetAddress> inetAddresses = localAddressList(address ->
				! isInnerIp(address)
				&& address instanceof Inet4Address);
		Optional<InetAddress> first = inetAddresses.stream().findFirst();
		String ip = first.map(InetAddress::getHostAddress).orElse(null);
		if (!StringUtil.isEmpty(ip)) {
			// 先读取本地网络配置的
			return ip;
		}

		// 下面的ip都可以获得ip地址.
		// whatismyip.akamai.com
		// ipecho.net/plain
		// v4.ident.me
		// ident.me
		// ip.sb
		String[] IPV4_SERVICES = {
				"https://checkip.amazonaws.com",
				"https://ipv4.icanhazip.com",
				"https://icanhazip.com",
				"https://ipinfo.io/ip"
		};

		List<Callable<String>> callables = Stream.of(IPV4_SERVICES).map(str -> (Callable<String>) () -> {
			try (BufferedReader in = new BufferedReader(new InputStreamReader(new URL(str).openStream()))) {
				String result = in.readLine();

				LoggerType.DUODUO.error("Get public ip {} from {}!", result, str);
				if (!NetUtil.isValidIp4(result))  {
					throw new CustomException("ip invalid!");
				}
				return result;
			}
		}).toList();

		try {
			String publicIp = ThreadPoolManager.NORMAL.invokeAny(callables);
			LoggerType.DUODUO.error("current public ip: {}", publicIp);
			return publicIp;
		} catch (InterruptedException | ExecutionException e) {
			throw new CustomException("No public ip get!");
		}
	}

	/**
	 * 是否是合格的ip4
	 * @param host
	 * @return
	 */
	public static boolean isValidIp4(String host) {
		String[] strings = StringUtil.split(host, ".");
		if (strings.length != 4) {
			return false;
		}
		for (int i = 0; i < strings.length; i++) {
			if (! StringUtil.isNum(strings[i])) {
				return false;
			}
			int i1 = Integer.parseInt(strings[i]);
			if (i1 < 0 || i1 > 255) {
				return false;
			}
		}
		return true;
	}
	/**
	 * 是否是合格的ip6
	 * @param inet6Address
	 * @return
	 */
	public static boolean isValidIp6(String inet6Address) {
		String[] parts;
		// remove prefix size. This will appear after the zone id (if any)
		parts = inet6Address.split("/", -1);
		if (parts.length > 2) {
			return false; // can only have one prefix specifier
		}
		if (parts.length == 2) {
			if (!parts[1].matches("\\d{1,3}")) {
				return false; // not a valid number
			}
			final int bits = Integer.parseInt(parts[1]); // cannot fail because of RE check
			if (bits < 0 || bits > MAX_BYTE) {
				return false; // out of range
			}
		}
		// remove zone-id
		parts = parts[0].split("%", -1);
		if (parts.length > 2) {
			return false;
		}
		// The id syntax is implementation independent, but it presumably cannot allow:
		// whitespace, '/' or '%'
		if ((parts.length == 2) && !parts[1].matches("[^\\s/%]+")) {
			return false; // invalid id
		}
		inet6Address = parts[0];
		final boolean containsCompressedZeroes = inet6Address.contains("::");
		if (containsCompressedZeroes && (inet6Address.indexOf("::") != inet6Address.lastIndexOf("::"))) {
			return false;
		}
		if ((inet6Address.startsWith(":") && !inet6Address.startsWith("::"))
				|| (inet6Address.endsWith(":") && !inet6Address.endsWith("::"))) {
			return false;
		}
		String[] octets = inet6Address.split(":");
		if (containsCompressedZeroes) {
			final List<String> octetList = new ArrayList<>(Arrays.asList(octets));
			if (inet6Address.endsWith("::")) {
				// String.split() drops ending empty segments
				octetList.add("");
			} else if (inet6Address.startsWith("::") && !octetList.isEmpty()) {
				octetList.remove(0);
			}
			octets = octetList.toArray(new String[0]);
		}
		if (octets.length > IPV6_MAX_HEX_GROUPS) {
			return false;
		}
		int validOctets = 0;
		int emptyOctets = 0; // consecutive empty chunks
		for (int index = 0; index < octets.length; index++) {
			final String octet = octets[index];
			if (octet.isEmpty()) {
				emptyOctets++;
				if (emptyOctets > 1) {
					return false;
				}
			} else {
				emptyOctets = 0;
				// Is last chunk an IPv4 address?
				if (index == octets.length - 1 && octet.contains(".")) {
					if (!isValidIp4(octet)) {
						return false;
					}
					validOctets += 2;
					continue;
				}
				if (octet.length() > IPV6_MAX_HEX_DIGITS_PER_GROUP) {
					return false;
				}
				int octetInt = 0;
				try {
					octetInt = Integer.parseInt(octet, BASE_16);
				} catch (final NumberFormatException e) {
					return false;
				}
				if (octetInt < 0 || octetInt > MAX_UNSIGNED_SHORT) {
					return false;
				}
			}
			validOctets++;
		}
		return validOctets <= IPV6_MAX_HEX_GROUPS && (validOctets >= IPV6_MAX_HEX_GROUPS || containsCompressedZeroes);
	}

	/**
	 * 使用Socket发送数据
	 *
	 * @param host Server主机
	 * @param port Server端口
	 * @param data 数据
	 */
	public static void socketSendData(String host, int port, byte[] data) throws IOException {
		try (SocketChannel channel = SocketChannel.open(InetSocketAddress.createUnresolved(host, port))) {
			channel.write(ByteBuffer.wrap(data));
		}
	}
	/**
	 * 使用udp发送数据
	 *
	 * @param host Server主机
	 * @param port Server端口
	 * @param data 数据
	 */
	public static void udpSendData(String host, int port, byte[] data) throws IOException {
		try (DatagramChannel channel = DatagramChannel.open()){
			channel.send(ByteBuffer.wrap(data), new InetSocketAddress(host, port));
		}
	}
	/***
	 * 得到主机名
	 * @return
	 */
	public static String getLocalHostName() {
		InetAddress address = null;
		try {
			address = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			LoggerType.DUODUO.error("", e);
		}
		return address != null ? address.getHostName() : null;
	}

	/**
	 * 获得本机所有的ip
	 * @return
	 */
	public static String[] getAllInnerIp(){
		LinkedHashSet<InetAddress> inetAddresses = localAddressList(i -> true);
		return toIpList(inetAddresses).toArray(new String[0]);
	}
	/**
	 * 获得本机的IPv4地址列表<br>
	 * 返回的IP列表有序，按照系统设备顺序
	 *
	 * @return IP地址列表 {@link LinkedHashSet}
	 */
	public static LinkedHashSet<String> localIpv4s() {
		final LinkedHashSet<InetAddress> localAddressList = localAddressList(t -> t instanceof Inet4Address);
		return toIpList(localAddressList);
	}

	/**
	 * 获得本机的IPv6地址列表<br>
	 * 返回的IP列表有序，按照系统设备顺序
	 *
	 * @return IP地址列表 {@link LinkedHashSet}
	 * @since 4.5.17
	 */
	public static LinkedHashSet<String> localIpv6s() {
		final LinkedHashSet<InetAddress> localAddressList = localAddressList(t -> t instanceof Inet6Address);
		return toIpList(localAddressList);
	}
	/**
	 * 地址列表转换为IP地址列表
	 *
	 * @param addressList 地址{@link Inet4Address} 列表
	 * @return IP地址字符串列表
	 * @since 4.5.17
	 */
	public static LinkedHashSet<String> toIpList(Set<InetAddress> addressList) {
		final LinkedHashSet<String> ipSet = new LinkedHashSet<>();
		for (InetAddress address : addressList) {
			ipSet.add(address.getHostAddress());
		}

		return ipSet;
	}
	/**
	 * 获取所有满足过滤条件的本地IP地址对象
	 *
	 * @param addressFilter 过滤器，null表示不过滤，获取所有地址
	 * @return 过滤后的地址对象列表
	 * @since 4.5.17
	 */
	public static LinkedHashSet<InetAddress> localAddressList(Predicate<InetAddress> addressFilter) {
		Enumeration<NetworkInterface> networkInterfaces;
		try {
			networkInterfaces = NetworkInterface.getNetworkInterfaces();
		} catch (SocketException e) {
			throw new RuntimeException(e);
		}

        final LinkedHashSet<InetAddress> ipSet = new LinkedHashSet<>();

		while (networkInterfaces.hasMoreElements()) {
			final NetworkInterface networkInterface = networkInterfaces.nextElement();
			final Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
			while (inetAddresses.hasMoreElements()) {
				final InetAddress inetAddress = inetAddresses.nextElement();
				if (inetAddress != null && (null == addressFilter || addressFilter.test(inetAddress))) {
					ipSet.add(inetAddress);
				}
			}
		}

		return ipSet;
	}
}
