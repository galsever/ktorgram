export async function request(
    path: string,
    method: string,
    inputBody: string,
    params: string[]
) {
    const res = await fetch(path + params.join('/'), {
        method: method,
        headers: {
            'Content-Type': 'application/json'
        },
        body: inputBody
    })
    return await res.json()
}