package info.u_team.gltf_parser;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import info.u_team.gltf_parser.generated.gltf.Buffer;
import info.u_team.gltf_parser.generated.gltf.BufferView;
import info.u_team.gltf_parser.generated.gltf.GlTF;

/**
 * Implementation of {@link GLTFParser} for binary (.glb) files
 * 
 * @author MrTroble
 * @version 1.0.0
 */
public class BinaryGLTFParser extends GLTFParser {
	
	/**
	 * {@link GLTFParser#GLTFParser(byte[])}
	 */
	public BinaryGLTFParser(byte[] data) {
		super(data);
	}
	
	/**
	 * {@link GLTFParser#GLTFParser(byte[], int, int)}
	 */
	public BinaryGLTFParser(byte[] data, int offset, int lenght) {
		super(data, offset, lenght);
	}
	
	private static final int GLTF_MAGIC_HEADER = 0x46546C67;
	private static final int GLTF_SUPPORTED_VERSION = 2;
	private static final int GLTF_CHUNK_BINARY = 0x004E4942;
	private static final int GLTF_CHUNK_JSON = 0x4E4F534A;
	
	private byte[] gltfToBinaryData;
	
	/**
	 * Parses the given data as binary glb files See: {@link GLTFParser#parse()}
	 */
	@Override
	public GlTF parse() throws IOException, GLTFParseException {
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		final int magic = buffer.getInt();
		if (magic != GLTF_MAGIC_HEADER)
			throw new GLTFParseException("Header magic does not match 'gltf' string!");
		
		final int version = buffer.getInt();
		if (version != GLTF_SUPPORTED_VERSION)
			throw new GLTFParseException("Version does not match, only '2' is supported!");
		
		final int length = buffer.getInt();
		if (length != buffer.limit())
			throw new GLTFParseException("Gltf header length does not match the parameter bLength!");
		
		final int chunkLengthJson = buffer.getInt();
		final int chunkTypeJson = buffer.getInt();
		final byte[] chunkDataJson = new byte[chunkLengthJson];
		buffer.get(chunkDataJson);
		if (chunkTypeJson != GLTF_CHUNK_JSON)
			throw new GLTFParseException("First chunk is not a json chunk!");
		final GlTF gltf = fromJson(chunkDataJson, 0, chunkDataJson.length).parse();
		
		final int chunkLengthBin = buffer.getInt();
		final int chunkTypeBin = buffer.getInt();
		gltfToBinaryData = new byte[chunkLengthBin];
		buffer.get(gltfToBinaryData);
		if (chunkTypeBin != GLTF_CHUNK_BINARY)
			throw new GLTFParseException("Second chunk is not a binary chunk!");
		
		final Buffer binBuffer = gltf.getBuffers().get(0);
		if (binBuffer.getUri() != null)
			throw new GLTFParseException("Buffer not a internal binary buffer!");
		
		this.gltf = gltf;
		return gltf;
	}
	
	/**
	 * {@link GLTFParser#getData(Buffer)}
	 */
	@Override
	public ByteBuffer getData(Buffer buffer) {
		return ByteBuffer.wrap(gltfToBinaryData);
	}
	
	/**
	 * {@link GLTFParser#getData(BufferView)}
	 */
	@Override
	public ByteBuffer getData(BufferView bufferView) {
		final int bufferIndex = ((Number) bufferView.getBuffer()).intValue();
		if (bufferIndex != 0)
			throw new UnsupportedOperationException("Index other then 0 not allowed in binary mode!");
		final ByteBuffer data = ByteBuffer.wrap(gltfToBinaryData);
		data.position(data.position() + bufferView.getByteOffset());
		data.limit(data.position() + bufferView.getByteLength());
		return data;
	}
	
	@Override
	public void close() throws Exception {
		gltfToBinaryData = null;
	}
}