import { strict as assert } from 'node:assert';
import { readFileSync } from 'node:fs';
import { test } from 'node:test';
import ts from 'typescript';

const source = readFileSync(new URL('./kyc.ts', import.meta.url), 'utf8');
const compiled = ts.transpileModule(source, {
  compilerOptions: { module: ts.ModuleKind.CommonJS },
}).outputText;
const service = {};
const api = { API_BASE_URL: 'http://localhost:8080', request: () => { throw new Error('Unexpected request'); } };
new Function('require', 'exports', compiled)(
  (id) => {
    assert.equal(id, './api');
    return api;
  },
  service,
);

test('loads the full document number from the staff review endpoint', async (t) => {
  t.mock.method(api, 'request', async (path) => {
    assert.equal(path, '/admin/kyc/verifications/42/document-number');
    return { documentNumber: '123456789V' };
  });
  assert.deepEqual(await service.getKycReviewDocumentNumber(42), { documentNumber: '123456789V' });
});

test('submits the front image and identity details as multipart data', async (t) => {
  const frontImage = new File(['front'], 'front.jpg', { type: 'image/jpeg' });
  const backImage = new File(['back'], 'back.png', { type: 'image/png' });
  const submission = {
    documentType: 'NIC',
    documentNumber: '123456789V',
    fullNameOnDocument: 'Test User',
  };
  t.mock.method(globalThis, 'fetch', async (url, options) => {
    assert.equal(url, 'http://localhost:8080/api/v1/kyc/verifications');
    assert.equal(options.method, 'POST');
    assert.deepEqual(JSON.parse(await options.body.get('submission').text()), submission);
    assert.equal(options.body.get('frontImage'), frontImage);
    assert.equal(options.body.get('backImage'), backImage);
    assert.equal(options.body.get('selfie'), null);
    return Response.json({ id: 1 });
  });

  assert.deepEqual(await service.submitKyc(submission, { frontImage, backImage }), { id: 1 });
});

test('surfaces the server error on unsuccessful submission', async (t) => {
  t.mock.method(globalThis, 'fetch', async () =>
    Response.json({ message: 'Unsupported file type' }, { status: 400 }),
  );
  await assert.rejects(
    service.submitKyc({}, { frontImage: new File(['front'], 'front.jpg') }),
    /Unsupported file type/,
  );
});

test('loads a review document with authentication and preserves its content type', async (t) => {
  const previousWindow = globalThis.window;
  const previousStorage = globalThis.localStorage;
  globalThis.window = {};
  globalThis.localStorage = { getItem: () => 'reviewer-token' };
  t.after(() => {
    if (previousWindow === undefined) delete globalThis.window;
    else globalThis.window = previousWindow;
    if (previousStorage === undefined) delete globalThis.localStorage;
    else globalThis.localStorage = previousStorage;
  });
  let document;
  t.mock.method(URL, 'createObjectURL', (blob) => {
    document = blob;
    return 'blob:review-document';
  });
  t.mock.method(globalThis, 'fetch', async (url, options) => {
    assert.equal(url, 'http://localhost:8080/api/v1/admin/kyc/verifications/42/images/FRONT');
    assert.equal(options.headers.Authorization, 'Bearer reviewer-token');
    return new Response(new Blob(['document'], { type: 'application/pdf' }));
  });
  const image = await service.getKycImageUrl(42, 'FRONT');
  assert.deepEqual(image, { url: 'blob:review-document', contentType: 'application/pdf' });
  assert.equal(await document.text(), 'document');
});

test('reports an unavailable review document', async (t) => {
  t.mock.method(globalThis, 'fetch', async () =>
    Response.json({ code: 'FILE_NOT_FOUND', message: 'Stored file is no longer available' }, { status: 400 }),
  );
  await assert.rejects(service.getKycImageUrl(42, 'BACK'), /Stored file is no longer available \(HTTP 400\)/);
});

test('reports the HTTP status when the server does not return a readable error', async (t) => {
  t.mock.method(globalThis, 'fetch', async () =>
    new Response('invalid json', { status: 502, headers: { 'Content-Type': 'application/json' } }),
  );
  await assert.rejects(service.getKycImageUrl(42, 'FRONT'), /Failed to load KYC image \(HTTP 502\)/);
});
