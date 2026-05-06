from dataclasses import dataclass


@dataclass(frozen=True)
class DeviceInfo:
    torch_cuda: bool
    onnx_gpu: bool


def detect_device(allow_gpu: bool) -> DeviceInfo:
    torch_cuda = False
    onnx_gpu = False
    if allow_gpu:
        try:
            import torch

            torch_cuda = torch.cuda.is_available()
        except Exception:
            torch_cuda = False
        try:
            import onnxruntime as ort

            onnx_gpu = ort.get_device().upper() == "GPU"
        except Exception:
            onnx_gpu = False
    return DeviceInfo(torch_cuda=torch_cuda, onnx_gpu=onnx_gpu)
